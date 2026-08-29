package com.getit.domain.dashboard.dto;

import java.util.List;

/** 주차별 과제 제출 현황. (API 명세서 5.3) */
public record SubmissionStatusResult(
    long totalMemberCount,
    List<WeekStat> weeks
) {

  public record WeekStat(
      Long lectureId,
      int week,
      String title,
      long submittedCount,
      long totalCount,
      double rate
  ) { }
}
