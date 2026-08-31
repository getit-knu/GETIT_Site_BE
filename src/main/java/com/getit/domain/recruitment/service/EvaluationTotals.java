package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.entity.EvaluationScore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 평가 점수를 "완료한 평가자들의 총점" 으로 접는다. (이슈 #188)
 *
 * <p>규칙은 상세(7.3)와 같다. <b>현재 기준을 모두 매긴 평가자만</b> 센다.
 *
 * <p>완료 판정을 <b>기준 개수</b>로 하기 때문에, 현재 목록에 없는 기준의 점수가 섞이면
 * 다 매기지 않은 평가자도 개수가 맞아 완료로 잡히고 옛 점수가 총점에 들어간다 (PR #154).
 * 지금은 그런 행이 남지 않는다 — 기준을 지우면 FK 의 {@code ON DELETE CASCADE}(V28)와
 * {@code EvaluationCriterionService} 의 명시적 정리가 점수도 함께 지운다. 판정이 개수에
 * 기대는 한 방어는 남겨 둔다.
 *
 * <p>일부만 매긴 평가자는 빼야 한다. 넣으면 그 사람 총점이 낮게 나와 평균이 실제보다
 * 내려간다.
 *
 * <p>목록(7.1)과 상세(7.3)가 다른 값을 보여주면 안 되므로 규칙을 한 곳에 둔다.
 */
final class EvaluationTotals {

  private EvaluationTotals() {
  }

  /**
   * 지원서별 "완료한 평가자들의 총점" 목록.
   *
   * @param criterionIds 현재 기수의 평가 기준 id. 비어 있으면 완료를 판정할 수 없어 빈 결과다
   */
  static Map<Long, List<Integer>> byApplication(
      Collection<EvaluationScore> scores, Set<Long> criterionIds
  ) {
    if (criterionIds.isEmpty()) {
      return Map.of();
    }

    return scores.stream()
        .filter(score -> criterionIds.contains(score.getCriterionId()))
        .collect(Collectors.groupingBy(EvaluationScore::getApplicationId))
        .entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey,
            entry -> completedTotals(entry.getValue(), criterionIds.size())));
  }

  /** 지원자 전체 평균. 평가가 끝난 지원자가 없으면 empty. */
  static OptionalDouble average(Collection<List<Integer>> totalsByApplication) {
    return totalsByApplication.stream()
        .flatMap(List::stream)
        .mapToInt(Integer::intValue)
        .average();
  }

  private static List<Integer> completedTotals(List<EvaluationScore> scores, int criterionCount) {
    return scores.stream()
        .collect(Collectors.groupingBy(EvaluationScore::getEvaluatorId))
        .values().stream()
        .filter(own -> own.size() == criterionCount)
        .map(own -> own.stream().mapToInt(EvaluationScore::getScore).sum())
        .toList();
  }
}
