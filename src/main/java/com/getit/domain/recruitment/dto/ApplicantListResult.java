package com.getit.domain.recruitment.dto;

import com.getit.global.dto.PageResponse;

/**
 * 지원자 목록과 전체 집계. (7.1, 이슈 #188)
 *
 * <p>목록만으로는 어떤 지원자의 점수가 높은 편인지 알 수 없다. 비교 기준을 함께 준다.
 *
 * <p>이 값을 프론트가 계산할 수 없어서 서버가 준다 — 목록이 페이징되어 있어 현재 페이지로
 * 내면 페이지를 넘길 때마다 기준값이 달라진다.
 *
 * @param summary 필터와 무관하게 <b>지원자 전체</b> 기준이다. 걸러진 집합의 평균으로 두면
 *                필터를 바꿀 때마다 "높은 편" 의 뜻이 달라져 비교 기준으로 쓸 수 없다.
 *                제출 현황(8.6)이 "필터를 걸어도 전체 기준" 인 것과 같은 방식이다
 */
public record ApplicantListResult(
    PageResponse<ApplicantSummary> applicants,
    EvaluationOverview summary
) {

  /**
   * @param averageTotalScore 평가를 끝낸 지원자들의 총점 평균. 아무도 없으면 {@code null}
   * @param evaluatedCount 평가가 하나라도 완료된 지원자 수. 평균이 몇 명에서 나온 값인지
   *                       모르면 화면이 그 수치를 얼마나 믿을지 정할 수 없다
   */
  public record EvaluationOverview(Double averageTotalScore, int evaluatedCount) {
  }
}
