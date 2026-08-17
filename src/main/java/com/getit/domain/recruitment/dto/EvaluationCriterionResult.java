package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.EvaluationCriterion;

/** 평가 기준 조회 · 저장 결과. (API 명세서 6.8 · 6.9 · 6.10) */
public record EvaluationCriterionResult(
    Long id,
    Integer order,
    String name,
    String guideline,
    Integer maxScore
) {

  public static EvaluationCriterionResult from(EvaluationCriterion criterion) {
    return new EvaluationCriterionResult(
        criterion.getId(),
        criterion.getOrder(),
        criterion.getName(),
        criterion.getGuideline(),
        criterion.getMaxScore()
    );
  }
}
