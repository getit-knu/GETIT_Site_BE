package com.getit.domain.lecture.admin.dto;

import com.getit.domain.lecture.util.KstDateTimes;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.SubmissionStatus;
import java.time.OffsetDateTime;
import java.util.List;

public class SubmissionDetailResult {

  public record LectureSummary(Long id, String title) { }

  public record UserSummary(Long id, String name, String major) { }

  public record FileSummary(
      Long fileId,
      String fileName,
      String url,
      String previewUrl,
      String contentType,
      Long size,
      boolean previewable
  ) { }

  public record FeedbackItem(
      Long id,
      Long adminId,
      String adminName,
      String content,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {

    public static FeedbackItem of(Feedback feedback, String adminName) {
      return new FeedbackItem(
          feedback.getId(), feedback.getAdminId(), adminName, feedback.getContent(),
          KstDateTimes.toOffset(feedback.getCreatedAt()), KstDateTimes.toOffset(feedback.getUpdatedAt()));
    }
  }

  public record Navigation(Long current, long total, Long prevSubmissionId, Long nextSubmissionId) { }

  public record Detail(
      Long id,
      LectureSummary lecture,
      UserSummary user,
      FileSummary file,
      String linkUrl,
      String comment,
      OffsetDateTime submittedAt,
      SubmissionStatus status,
      List<FeedbackItem> feedbacks,
      Navigation navigation
  ) { }
}
