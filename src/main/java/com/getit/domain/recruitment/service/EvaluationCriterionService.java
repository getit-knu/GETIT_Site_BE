package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.EvaluationCriteriaSummary;
import com.getit.domain.recruitment.dto.EvaluationCriterionResult;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 서류 평가 기준 조회 · 설정. (API 명세서 6.8 · 6.9 · 6.10 · 6.11) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EvaluationCriterionService {

  /**
   * 배점 합계 상한. (API 명세서 6.9)
   *
   * <p>명세서 문구("추가·수정·삭제 모든 쓰기 시점에 합계 100 검증")를 문자 그대로 "항상 정확히
   * 100"으로 해석하면, 기준을 하나씩 추가하는 중간 상태(예: 20점만 추가된 시점)조차 저장할 수
   * 없어진다. 이 값을 넘을 때만 거부하는 방식으로 구현하고, 정확히 100인지는
   * {@link EvaluationCriteriaSummary#valid()} 로만 안내한다. 삭제는 합계를 줄이기만 하므로
   * 이 검증에 걸릴 일이 없다.
   */
  private static final int MAX_TOTAL_SCORE = 100;

  private final EvaluationCriterionRepository evaluationCriterionRepository;
  private final GenerationQueryService generationQueryService;

  public EvaluationCriteriaSummary getCriteria() {
    GenerationSummary activeGeneration = findActiveGeneration();

    List<EvaluationCriterionResult> criteria =
        evaluationCriterionRepository.findByGenerationId(activeGeneration.id()).stream()
            .map(EvaluationCriterionResult::from)
            .toList();

    return EvaluationCriteriaSummary.of(criteria);
  }

  @Transactional
  public EvaluationCriterionResult createCriterion(String name, String guideline, Integer maxScore) {
    GenerationSummary activeGeneration = findActiveGeneration();
    List<EvaluationCriterion> existing =
        evaluationCriterionRepository.findByGenerationId(activeGeneration.id());
    validateTotal(sumMaxScore(existing) + maxScore);

    int nextOrder = existing.size() + 1;
    EvaluationCriterion saved = evaluationCriterionRepository.save(
        EvaluationCriterion.create(activeGeneration.id(), nextOrder, name, guideline, maxScore));

    return EvaluationCriterionResult.from(saved);
  }

  @Transactional
  public EvaluationCriterionResult updateCriterion(
      Long criterionId, String name, String guideline, Integer maxScore
  ) {
    GenerationSummary activeGeneration = findActiveGeneration();
    EvaluationCriterion criterion = findCriterion(criterionId, activeGeneration.id());

    List<EvaluationCriterion> others =
        evaluationCriterionRepository.findByGenerationId(activeGeneration.id()).stream()
            .filter(c -> !c.getId().equals(criterionId))
            .toList();
    validateTotal(sumMaxScore(others) + maxScore);

    criterion.update(name, guideline, maxScore);

    return EvaluationCriterionResult.from(criterion);
  }

  /** 6.11. 삭제 후 뒤 순서를 한 칸씩 당겨서 order 중복을 막는다 (ApplicationQuestion 과 동일 이유). */
  @Transactional
  public void deleteCriterion(Long criterionId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    EvaluationCriterion criterion = findCriterion(criterionId, activeGeneration.id());
    int deletedOrder = criterion.getOrder();

    evaluationCriterionRepository.delete(criterion);

    evaluationCriterionRepository.findByGenerationId(activeGeneration.id()).stream()
        .filter(c -> c.getOrder() > deletedOrder)
        .forEach(c -> c.updateOrder(c.getOrder() - 1));
  }

  private void validateTotal(int total) {
    if (total > MAX_TOTAL_SCORE) {
      throw new BusinessException(
          RecruitmentErrorCode.INVALID_CRITERIA_TOTAL,
          "평가 기준 배점 합계는 100점을 초과할 수 없습니다. (현재 " + total + "점)");
    }
  }

  private int sumMaxScore(List<EvaluationCriterion> criteria) {
    return criteria.stream().mapToInt(EvaluationCriterion::getMaxScore).sum();
  }

  private EvaluationCriterion findCriterion(Long criterionId, Long activeGenerationId) {
    return evaluationCriterionRepository.findByIdAndGenerationId(criterionId, activeGenerationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.CRITERION_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
