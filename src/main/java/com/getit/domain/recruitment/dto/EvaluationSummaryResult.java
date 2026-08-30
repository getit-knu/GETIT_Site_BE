package com.getit.domain.recruitment.dto;

import java.util.List;

/**
 * 지원자 한 명의 종합 평가 결과. (이슈 #151)
 *
 * <h2>집계 방식</h2>
 *
 * <p>명세서 7.3 이 "복수 허용 시 {@code totalScore} 는 평가자별 총점의 평균으로 계산하고
 * {@code evaluatorCount} 를 추가한다" 고 정해두었다. 그대로 따른다.
 *
 * <p><b>모든 기준에 점수를 매긴 평가자만</b> 평균에 넣는다. 4개 기준 중 2개만 매긴 사람의
 * 총점을 다 매긴 사람의 총점과 나란히 평균 내면 실제보다 낮게 나온다.
 *
 * @param totalScore 완료한 평가자들의 총점 평균. 완료한 사람이 없으면 {@code null}
 * @param evaluatorCount 모든 기준을 매긴 평가자 수
 * @param myTotalScore 요청한 운영진 본인의 총점. 아직 다 매기지 않았으면 {@code null}
 */
public record EvaluationSummaryResult(
    Long applicationId,
    List<CriterionScore> criteria,
    Double totalScore,
    Integer evaluatorCount,
    Integer myTotalScore
) {

  /**
   * @param averageScore 이 기준에 점수를 매긴 사람들의 평균. 아무도 안 매겼으면 {@code null}
   * @param myScore 요청한 운영진 본인의 점수. 안 매겼으면 {@code null}
   * @param evaluatorScores 누가 몇 점을 줬는지. 종합만 보면 이견이 큰 기준을 알 수 없다
   */
  public record CriterionScore(
      Long criterionId,
      String criterionName,
      Integer maxScore,
      Double averageScore,
      Integer myScore,
      List<EvaluatorScore> evaluatorScores
  ) { }

  public record EvaluatorScore(Long evaluatorId, String evaluatorName, Integer score) { }
}
