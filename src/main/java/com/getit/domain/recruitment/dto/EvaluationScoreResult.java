package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.EvaluationCriterion;

/** 평가 기준 하나에 대한 점수. (API 명세서 7.3) {@code score} 가 null 이면 아직 채점되지 않은 것이다. */
public record EvaluationScoreResult(
    Long criterionId,
    String criterionName,
    Integer maxScore,
    Integer score
) {

  public static EvaluationScoreResult of(EvaluationCriterion criterion, Integer score) {
    return new EvaluationScoreResult(
        criterion.getId(),
        criterion.getName(),
        criterion.getMaxScore(),
        score
    );
  }
}
