package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.dto.ApplicationAnswerResult;
import com.getit.domain.recruitment.dto.DocumentDecisionResult;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreResult;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.entity.EvaluationScore;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 지원자 목록·상세 조회 · 서류 평가 저장 · 합불 처리. (API 명세서 7.1 · 7.2 · 7.3 · 7.4) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationAdminService {

  private final ApplicationRepository applicationRepository;
  private final ApplicationAnswerRepository applicationAnswerRepository;
  private final EvaluationCriterionRepository evaluationCriterionRepository;
  private final EvaluationScoreRepository evaluationScoreRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * 7.1. {@code generationId} 가 없으면 활성 기수를 대상으로 한다 (PR #48 리뷰 지적 — 예전에는
   * 항상 활성 기수만 조회할 수 있어서 7.2(상세, 기수 제한 없음)와 스코프가 달랐다. 관리자가
   * 지난 기수 지원자 상세에 도달할 경로가 없던 문제라 목록에서도 기수를 지정할 수 있게 했다).
   *
   * <p>status 필터가 없으면 아직 제출하지 않은(DRAFT) 지원서는 제외한다 — 임시 저장만 하고
   * 제출하지 않은 지원자는 심사 대상이 아니기 때문이다.
   */
  public PageResponse<ApplicantSummary> listApplicants(Long generationId, ApplicationStatus status, Pageable pageable) {
    Long targetGenerationId = generationId != null ? generationId : findActiveGeneration().id();

    Page<Application> applications = status != null
        ? applicationRepository.findByGenerationIdAndStatus(targetGenerationId, status, pageable)
        : applicationRepository.findByGenerationIdAndStatusNot(
            targetGenerationId, ApplicationStatus.DRAFT, pageable);

    return PageResponse.from(applications, ApplicantSummary::from);
  }

  /**
   * 7.2. 상세 조회는 기수 제한 없이 id 로만 찾는다 — 지난 기수 지원자의 상세도 조회할 수 있어야
   * 하기 때문이다 (목록과 달리 "활성 기수만" 제한을 두지 않는다).
   *
   * <p>DRAFT 는 조회 대상에서 제외한다 (PR #48 리뷰 지적 — 지원자가 작성 중 저장만 해도 관리자가
   * 미완성 답변을 그대로 읽을 수 있었다). {@link com.getit.domain.recruitment.service.ApplicationService#getResult}
   * 가 DRAFT 를 "제출한 지원서 없음"으로 취급하는 것과 같은 방식으로, 없는 지원서와 동일하게 404 로
   * 처리한다.
   */
  public ApplicantDetailResult getApplicantDetail(Long applicationId) {
    Application application = findEvaluableApplication(applicationId);

    List<ApplicationAnswerResult> answers =
        applicationAnswerRepository.findByApplicationId(application.getId()).stream()
            .map(ApplicationAnswerResult::from)
            .toList();

    return ApplicantDetailResult.of(application, answers);
  }

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

  /** 7.2 상세 · 7.3 채점 공용. DRAFT 는 심사 대상이 아니므로 없는 지원서와 동일하게 404 로 처리한다. */
  private Application findEvaluableApplication(Long applicationId) {
    return applicationRepository.findById(applicationId)
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
  }

  /**
   * 7.3 채점 · 7.4 합불 처리 공용. 둘 다 SUBMITTED 상태에서만 허용한다. DRAFT 는
   * {@link #findEvaluableApplication} 과 동일하게 없는 지원서처럼 404 로 처리하고(PR #52 리뷰
   * 지적 — 예전엔 decide 가 findById 를 직접 써서, DRAFT 지원서의 존재가 409 로 노출됐다),
   * SUBMITTED 가 아닌 다른 상태(이미 결정됨 등)는 409 로 구분한다.
   */
  private Application findSubmittedApplication(Long applicationId) {
    Application application = findEvaluableApplication(applicationId);
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

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
