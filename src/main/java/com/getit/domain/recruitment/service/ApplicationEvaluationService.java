package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.DocumentDecisionResult;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreResult;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.entity.EvaluationScore;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 서류 평가 점수 저장 · 합불 처리. (API 명세서 7.3 · 7.4)
 *
 * <p>{@code ApplicationAdminService}(7.1 · 7.2 · 7.5 · 7.6)가 이 메서드들까지 다 갖고 있으면
 * 300줄 제한을 넘어서(PR #54 작업 중 313줄) 별도 서비스로 분리했다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationEvaluationService {

  private final ApplicationRepository applicationRepository;
  private final EvaluationCriterionRepository evaluationCriterionRepository;
  private final EvaluationScoreRepository evaluationScoreRepository;

  /**
   * 7.3. 기준마다 upsert 한다. 기준은 지원서와 같은 기수 소속이어야 한다 — 다른 기수 기준으로
   * 점수를 매기면 9기 지원자에게 8기 평가 기준 점수가 매겨지는 것과 같은 문제가 생긴다 (PR #51 리뷰
   * 지적). 응답은 기수의 평가 기준 전체를 기준으로 반환해서, 아직 다 채점되지 않은 기준이 있는지
   * 클라이언트가 판단할 수 있게 한다.
   *
   * <p>제출(SUBMITTED) 상태에서만 허용한다 (PR #52 리뷰 지적 — 예전엔 합불 결정(DOC_PASS/
   * DOC_FAIL) 이후에도 점수를 계속 고칠 수 있어서, 결정 근거가 결정 이후에 바뀌는 정책 모순이
   * 있었다).
   */
  @Transactional
  public EvaluationScoreSaveResult saveScores(Long applicationId, List<EvaluationScoreItem> items) {
    Application application = findSubmittedApplication(applicationId);

    for (EvaluationScoreItem item : items) {
      EvaluationCriterion criterion = findCriterionInGeneration(item.criterionId(), application.getGenerationId());
      validateScore(item.score(), criterion.getMaxScore());
      upsertScore(applicationId, item.criterionId(), item.score());
    }

    return buildScoreSummary(application);
  }

  /**
   * 7.4. 서류 합불 처리. 제출(SUBMITTED) 상태에서만 가능하다 — 그 외 상태는 이미 처리됐거나
   * 대상이 아니다.
   *
   * <p>먼저 {@link #findSubmittedApplication} 으로 404(없음 · DRAFT) · 409(SUBMITTED 아님)를
   * 정확히 구분해서 던지고, 실제 갱신은 {@code updateStatusIfCurrentStatus} 로 원자적으로 한다
   * — 두 결정 요청이 동시에 들어와 앞의 확인을 둘 다 통과해도 실제 갱신은 하나만 성공한다
   * (PR #52 Copilot 리뷰 지적 — 확인과 갱신이 분리돼 있으면 lost update 가 날 수 있었다).
   */
  @Transactional
  public DocumentDecisionResult decide(Long applicationId, boolean passed) {
    findSubmittedApplication(applicationId);

    ApplicationStatus result = passed ? ApplicationStatus.DOC_PASS : ApplicationStatus.DOC_FAIL;
    int updated = applicationRepository.updateStatusIfCurrentStatus(
        applicationId, result, ApplicationStatus.SUBMITTED);
    if (updated == 0) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    return new DocumentDecisionResult(applicationId, result);
  }

  /**
   * 7.3 채점 · 7.4 합불 처리 공용. DRAFT 는 없는 지원서처럼 404 로 처리하고(PR #52 리뷰 지적 —
   * 예전엔 decide 가 이 확인 없이 findById 를 직접 써서, DRAFT 지원서의 존재가 409 로 노출됐다),
   * SUBMITTED 가 아닌 다른 상태(이미 결정됨 등)는 409 로 구분한다.
   */
  private Application findSubmittedApplication(Long applicationId) {
    Application application = applicationRepository.findById(applicationId)
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
    if (application.getStatus() != ApplicationStatus.SUBMITTED) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }
    return application;
  }

  /**
   * (applicationId, criterionId) upsert. 존재 확인과 삽입 사이에 다른 요청이 끼어들면 둘 다
   * "없음"으로 보고 동시에 삽입을 시도해 유니크 제약 위반(처리 안 하면 500)이 날 수 있다
   * (PR #52 Copilot 리뷰 지적). {@code saveAndFlush} 로 그 위반을 이 자리에서 즉시 잡아내고,
   * 그 경우 다른 요청이 먼저 넣은 행을 다시 조회해 갱신으로 전환한다.
   */
  private void upsertScore(Long applicationId, Long criterionId, Integer score) {
    Optional<EvaluationScore> existing =
        evaluationScoreRepository.findByApplicationIdAndCriterionId(applicationId, criterionId);
    if (existing.isPresent()) {
      existing.get().updateScore(score);
      return;
    }

    try {
      evaluationScoreRepository.saveAndFlush(EvaluationScore.create(applicationId, criterionId, score));
    } catch (DataIntegrityViolationException e) {
      evaluationScoreRepository.findByApplicationIdAndCriterionId(applicationId, criterionId)
          .orElseThrow(() -> e)
          .updateScore(score);
    }
  }

  private EvaluationCriterion findCriterionInGeneration(Long criterionId, Long generationId) {
    return evaluationCriterionRepository.findByIdAndGenerationId(criterionId, generationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.CRITERION_NOT_FOUND));
  }

  private void validateScore(Integer score, Integer maxScore) {
    if (score > maxScore) {
      throw new BusinessException(
          RecruitmentErrorCode.SCORE_EXCEEDS_MAX, "점수가 배점(" + maxScore + "점)을 초과했습니다.");
    }
  }

  private EvaluationScoreSaveResult buildScoreSummary(Application application) {
    List<EvaluationCriterion> criteria =
        evaluationCriterionRepository.findByGenerationId(application.getGenerationId());
    Map<Long, EvaluationScore> scoresByCriterionId =
        evaluationScoreRepository.findByApplicationId(application.getId()).stream()
            .collect(Collectors.toMap(EvaluationScore::getCriterionId, Function.identity()));

    List<EvaluationScoreResult> results = criteria.stream()
        .map(criterion -> {
          EvaluationScore score = scoresByCriterionId.get(criterion.getId());
          return EvaluationScoreResult.of(criterion, score != null ? score.getScore() : null);
        })
        .toList();

    boolean allScored = results.stream().allMatch(result -> result.score() != null);
    Integer totalScore = allScored
        ? results.stream().mapToInt(EvaluationScoreResult::score).sum()
        : null;

    return new EvaluationScoreSaveResult(application.getId(), results, totalScore);
  }
}
