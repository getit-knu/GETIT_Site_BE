package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignmentSubmissionTest {

  @Test
  @DisplayName("과제·제출자·파일·상태로 생성된다")
  void createsSubmission() {
    LocalDateTime submittedAt = LocalDateTime.of(2026, 6, 19, 12, 0, 0);

    AssignmentSubmission submission = AssignmentSubmission.submit(
        1L, 100L, 500L, null, "코멘트", SubmissionStatus.SUBMITTED, submittedAt);

    assertThat(submission.getAssignmentId()).isEqualTo(1L);
    assertThat(submission.getUserId()).isEqualTo(100L);
    assertThat(submission.getFileId()).isEqualTo(500L);
    assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
  }

  @Test
  @DisplayName("재제출 시 파일·링크·코멘트·상태·제출시각이 갱신된다")
  void resubmitsUpdatesFields() {
    AssignmentSubmission submission = AssignmentSubmission.submit(
        1L, 100L, 500L, null, "기존 코멘트", SubmissionStatus.SUBMITTED,
        LocalDateTime.of(2026, 6, 19, 12, 0, 0));

    LocalDateTime newSubmittedAt = LocalDateTime.of(2026, 6, 20, 12, 0, 0);
    submission.resubmit(null, "https://github.com/user/repo", "새 코멘트", SubmissionStatus.LATE, newSubmittedAt);

    assertThat(submission.getFileId()).isNull();
    assertThat(submission.getLinkUrl()).isEqualTo("https://github.com/user/repo");
    assertThat(submission.getComment()).isEqualTo("새 코멘트");
    assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.LATE);
    assertThat(submission.getSubmittedAt()).isEqualTo(newSubmittedAt);
  }

  @Test
  @DisplayName("제출자 본인인지 확인한다")
  void checksOwnership() {
    AssignmentSubmission submission = AssignmentSubmission.submit(
        1L, 100L, 500L, null, null, SubmissionStatus.SUBMITTED, LocalDateTime.now());

    assertThat(submission.isOwnedBy(100L)).isTrue();
    assertThat(submission.isOwnedBy(999L)).isFalse();
  }
}
