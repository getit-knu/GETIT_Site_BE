package com.getit.domain.lecture.admin.dto;

import com.getit.domain.lecture.util.KstDateTimes;
import com.getit.domain.lecture.entity.SubmissionStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public class SubmissionOverviewResult {

  public record LectureSummary(Long id, String title, OffsetDateTime deadline) {

    public static LectureSummary of(Long id, String title, LocalDateTime deadline) {
      return new LectureSummary(id, title, KstDateTimes.toOffset(deadline));
    }
  }

  public record Counts(long submitted, long notSubmitted, long total) { }

  public record Row(
      Long userId,
      String userName,
      String major,
      Long submissionId,
      boolean submitted,
      SubmissionStatus status,
      OffsetDateTime submittedAt,
      boolean feedbackDone
  ) { }

  public record Overview(
      LectureSummary lecture,
      Counts counts,
      List<Row> content,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last
  ) {

    public static Overview of(LectureSummary lecture, Counts counts, Page<Row> page) {
      return new Overview(
          lecture, counts, page.getContent(), page.getNumber(), page.getSize(),
          page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
    }
  }
}
