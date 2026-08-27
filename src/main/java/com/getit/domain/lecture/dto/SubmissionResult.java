package com.getit.domain.lecture.dto;

import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.SubmissionStatus;
import java.time.LocalDateTime;

public class SubmissionResult {

  public record Detail(
      Long id,
      Long assignmentId,
      String fileUrl,
      String fileName,
      String linkUrl,
      String comment,
      LocalDateTime submittedAt,
      SubmissionStatus status
  ) {

    public static Detail of(AssignmentSubmission submission, String fileUrl, String fileName) {
      return new Detail(
          submission.getId(), submission.getAssignmentId(), fileUrl, fileName, submission.getLinkUrl(),
          submission.getComment(), submission.getSubmittedAt(), submission.getStatus());
    }
  }
}
