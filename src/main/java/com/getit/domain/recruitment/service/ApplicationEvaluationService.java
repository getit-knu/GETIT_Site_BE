package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.BulkDecisionResult;
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
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
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
  private final GenerationQueryService generationQueryService;

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
   * 7.4. 합불 처리. {@code SUBMITTED}(→ 서류 합불) · {@code DOC_PASS}(→ 최종 합불, 7.4 확장) 단계
   * 둘 다에서 호출할 수 있다 — 어느 단계인지는 지원서의 현재 상태로 판단하고, 클라이언트는 여전히
   * {@code passed} 불리언 하나만 보낸다(명세서 원문은 목표 status 를 직접 받는 형태지만, 기존
   * 구현이 이미 이 불리언 시그니처로 나가 있어(PR #52) 굳이 바꾸지 않는다).
   *
   * <p>먼저 {@link #findDecidableApplication} 으로 404(없음 · DRAFT) · 409(결정 가능한 상태
   * 아님)를 정확히 구분해서 던지고, 실제 갱신은 {@code updateStatusIfCurrentStatus} 로 원자적으로
   * 한다 — 두 결정 요청이 동시에 들어와 앞의 확인을 둘 다 통과해도 실제 갱신은 하나만 성공한다
   * (PR #52 Copilot 리뷰 지적 — 확인과 갱신이 분리돼 있으면 lost update 가 날 수 있었다).
   */
  @Transactional
  public DocumentDecisionResult decide(Long applicationId, boolean passed) {
    Application application = findDecidableApplication(applicationId);
    ApplicationStatus current = application.getStatus();
    ApplicationStatus target = nextDecisionStatus(current, passed);

    int updated = applicationRepository.updateStatusIfCurrentStatus(applicationId, target, current);
    if (updated == 0) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    return new DocumentDecisionResult(applicationId, target);
  }

  /**
   * 7.4 일괄 처리(확장). {@code applicationIds} 중 {@code targetStatus} 의 선행 상태(SUBMITTED →
   * DOC_PASS/DOC_FAIL, DOC_PASS → FINAL_PASS/FINAL_FAIL)인 것만 원자적으로 갱신한다. 대상이
   * 아니었던 id 는 명세서 응답에 skip 목록이 없으므로 조용히 건너뛰고 {@code updatedCount} 로만
   * 반영한다 — 9.4(승격)의 skip 목록과 다른 점이다.
   *
   * <p>요청 본문엔 기수가 없어서, 활성 기수를 직접 구해 조건에 넣는다 — 그러지 않으면 비활성
   * (과거) 기수의 지원서까지 함께 바뀔 수 있다 (PR #69 Copilot 리뷰 지적).
   */
  @Transactional
  public BulkDecisionResult decideBulk(List<Long> applicationIds, ApplicationStatus targetStatus) {
    ApplicationStatus requiredStatus = requiredPredecessorOf(targetStatus);
    GenerationSummary activeGeneration = findActiveGeneration();

    int updated = applicationRepository.updateStatusIfCurrentStatusIn(
        applicationIds, targetStatus, requiredStatus, activeGeneration.id());

    return new BulkDecisionResult(updated, targetStatus);
  }

  /**
   * 7.3 채점 전용. DRAFT 는 없는 지원서처럼 404 로 처리하고(PR #52 리뷰 지적 — 예전엔 decide 가
   * 이 확인 없이 findById 를 직접 써서, DRAFT 지원서의 존재가 409 로 노출됐다), SUBMITTED 만
   * 허용한다. {@code APPLICATION_NOT_SUBMITTED} 는 decide(7.4) 전용 코드라 여기서 재사용하면
   * "제출됨 또는 서류합격 상태만 결정 가능"이라는 메시지가 채점 거부 이유와 모순된다 — 채점
   * 전용 코드({@code APPLICATION_NOT_SCORABLE})를 따로 쓴다 (PR #69 Copilot 리뷰 지적).
   */
  private Application findSubmittedApplication(Long applicationId) {
    Application application = applicationRepository.findById(applicationId)
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
    if (application.getStatus() != ApplicationStatus.SUBMITTED) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SCORABLE);
    }
    return application;
  }

  /**
   * decide(7.4) 전용. 활성 기수로 스코프한다 — id 만으로 찾으면 비활성(과거) 기수의 DOC_PASS
   * 지원서도 FINAL_PASS/FINAL_FAIL 로 바꿀 수 있다 (PR #69 Copilot 리뷰 지적 —
   * {@code EvaluationCriterionService.findCriterion} 과 동일한 패턴). 다른 기수의 지원서는
   * 존재해도 404 로 처리하고, SUBMITTED · DOC_PASS 가 아니면 409 로 처리한다.
   */
  private Application findDecidableApplication(Long applicationId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    Application application = applicationRepository
        .findByIdAndGenerationId(applicationId, activeGeneration.id())
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
    if (application.getStatus() != ApplicationStatus.SUBMITTED && application.getStatus() != ApplicationStatus.DOC_PASS) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }
    return application;
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  private ApplicationStatus nextDecisionStatus(ApplicationStatus current, boolean passed) {
    return switch (current) {
      case SUBMITTED -> passed ? ApplicationStatus.DOC_PASS : ApplicationStatus.DOC_FAIL;
      case DOC_PASS -> passed ? ApplicationStatus.FINAL_PASS : ApplicationStatus.FINAL_FAIL;
      default -> throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    };
  }

  /** 7.4 일괄 처리 전용. 목표 status 에서 요구되는 현재 상태를 구한다 (허용 전이 표, 명세서 7.4). */
  private ApplicationStatus requiredPredecessorOf(ApplicationStatus targetStatus) {
    return switch (targetStatus) {
      case DOC_PASS, DOC_FAIL -> ApplicationStatus.SUBMITTED;
      case FINAL_PASS, FINAL_FAIL -> ApplicationStatus.DOC_PASS;
      default -> throw new BusinessException(RecruitmentErrorCode.INVALID_DECISION_STATUS);
    };
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
