package com.getit.domain.recruitment.dto;

import java.util.List;

/**
 * 평가 기준 목록 조회 결과. (API 명세서 6.8)
 *
 * <p>{@code valid} 는 {@code totalScore == 100} 여부다. 프론트가 "총점: 100점" 표시에 사용한다.
 */
public record EvaluationCriteriaSummary(
    List<EvaluationCriterionResult> criteria,
    int totalScore,
    boolean valid
) {

  private static final int VALID_TOTAL_SCORE = 100;

  public static EvaluationCriteriaSummary of(List<EvaluationCriterionResult> criteria) {
    int totalScore = criteria.stream().mapToInt(EvaluationCriterionResult::maxScore).sum();
    return new EvaluationCriteriaSummary(criteria, totalScore, totalScore == VALID_TOTAL_SCORE);
  }
}
