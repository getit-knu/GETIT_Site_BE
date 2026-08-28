package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeedbackTest {

  @Test
  @DisplayName("제출물·작성자·내용으로 생성된다")
  void createsFeedback() {
    Feedback feedback = Feedback.create(1L, 100L, "잘했습니다");

    assertThat(feedback.getSubmissionId()).isEqualTo(1L);
    assertThat(feedback.getAdminId()).isEqualTo(100L);
    assertThat(feedback.getContent()).isEqualTo("잘했습니다");
  }

  @Test
  @DisplayName("내용을 수정한다")
  void updatesContent() {
    Feedback feedback = Feedback.create(1L, 100L, "잘했습니다");

    feedback.update("수정된 피드백");

    assertThat(feedback.getContent()).isEqualTo("수정된 피드백");
  }

  @Test
  @DisplayName("작성자 본인인지 확인한다")
  void checksAuthorship() {
    Feedback feedback = Feedback.create(1L, 100L, "잘했습니다");

    assertThat(feedback.isWrittenBy(100L)).isTrue();
    assertThat(feedback.isWrittenBy(999L)).isFalse();
  }
}
