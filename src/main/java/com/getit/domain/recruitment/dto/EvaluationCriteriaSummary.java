package com.getit.domain.recruitment.dto;

import java.util.List;

/**
 * 평가 기준 목록 조회 결과. (API 명세서 6.8)
 *
 * <p>{@code valid} 는 {@code totalScore == MAX_TOTAL_SCORE} 여부다. 프론트가 "총점: 100점" 표시에
 * 사용한다. {@link com.getit.domain.recruitment.service.EvaluationCriterionService} 의 쓰기 검증도
 * 같은 값을 참조한다 — 배점 상한이 바뀌어도 한 곳만 고치면 되도록 여기를 유일한 기준으로 둔다.
 */
public record EvaluationCriteriaSummary(
    List<EvaluationCriterionResult> criteria,
    int totalScore,
    boolean valid
) {

  public static final int MAX_TOTAL_SCORE = 100;

  public static EvaluationCriteriaSummary of(List<EvaluationCriterionResult> criteria) {
    int totalScore = criteria.stream().mapToInt(EvaluationCriterionResult::maxScore).sum();
    return new EvaluationCriteriaSummary(criteria, totalScore, totalScore == MAX_TOTAL_SCORE);
  }
}
