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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
   */
  @Transactional
  public EvaluationScoreSaveResult saveScores(Long applicationId, List<EvaluationScoreItem> items) {
    Application application = findEvaluableApplication(applicationId);

    for (EvaluationScoreItem item : items) {
      EvaluationCriterion criterion = findCriterionInGeneration(item.criterionId(), application.getGenerationId());
      validateScore(item.score(), criterion.getMaxScore());

      evaluationScoreRepository.findByApplicationIdAndCriterionId(applicationId, item.criterionId())
          .ifPresentOrElse(
              existing -> existing.updateScore(item.score()),
              () -> evaluationScoreRepository.save(
                  EvaluationScore.create(applicationId, item.criterionId(), item.score())));
    }

    return buildScoreSummary(application);
  }

  /** 7.4. 서류 합불 처리. 제출(SUBMITTED) 상태에서만 가능하다 — 그 외 상태는 이미 처리됐거나 대상이 아니다. */
  @Transactional
  public DocumentDecisionResult decide(Long applicationId, boolean passed) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

    if (application.getStatus() != ApplicationStatus.SUBMITTED) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    application.decideDocumentResult(passed);
    return new DocumentDecisionResult(application.getId(), application.getStatus());
  }

  /** 7.2 상세 · 7.3 채점 공용. DRAFT 는 심사 대상이 아니므로 없는 지원서와 동일하게 404 로 처리한다. */
  private Application findEvaluableApplication(Long applicationId) {
    return applicationRepository.findById(applicationId)
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
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

    int totalScore = results.stream()
        .filter(result -> result.score() != null)
        .mapToInt(EvaluationScoreResult::score)
        .sum();

    return new EvaluationScoreSaveResult(application.getId(), results, totalScore);
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
