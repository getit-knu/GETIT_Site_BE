package com.getit.domain.lecture.admin.dto;

import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.util.KstDateTimes;
import java.time.OffsetDateTime;

public class FeedbackResult {

  public record CreateResult(Long id, Long submissionId, String adminName, String content, OffsetDateTime createdAt) {

    public static CreateResult of(Feedback feedback, String adminName) {
      return new CreateResult(
          feedback.getId(), feedback.getSubmissionId(), adminName, feedback.getContent(),
          KstDateTimes.toOffset(feedback.getCreatedAt()));
    }
  }

  public record UpdateResult(Long id, String content, OffsetDateTime updatedAt) {

    public static UpdateResult from(Feedback feedback) {
      return new UpdateResult(feedback.getId(), feedback.getContent(), KstDateTimes.toOffset(feedback.getUpdatedAt()));
    }
  }
}
