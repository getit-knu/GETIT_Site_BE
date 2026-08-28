package com.getit.domain.lecture.dto;

import java.util.List;

public class MeSummaryResult {

  public record Response(
      Profile profile,
      Stats stats,
      List<LectureBrief> notSubmittedLectures,
      List<LectureBrief> lateSubmittedLectures
  ) { }

  public record Profile(
      String name,
      String email,
      String college,
      String major,
      String studentId,
      Integer studentYear,
      String profileImageUrl
  ) { }

  public record Stats(
      long enrolledLectureCount,
      long submittedAssignmentCount,
      long notSubmittedCount,
      long lateSubmittedCount
  ) { }

  public record LectureBrief(Long lectureId, Integer week, String title) { }
}
