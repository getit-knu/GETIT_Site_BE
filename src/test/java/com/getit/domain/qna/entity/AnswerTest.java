package com.getit.domain.qna.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnswerTest {

  @Test
  @DisplayName("생성한다")
  void creates() {
    Answer answer = Answer.create(10L, 100L, "답변입니다");

    assertThat(answer.getQuestionId()).isEqualTo(10L);
    assertThat(answer.getAdminId()).isEqualTo(100L);
    assertThat(answer.getContent()).isEqualTo("답변입니다");
  }

  @Test
  @DisplayName("수정한다")
  void updates() {
    Answer answer = Answer.create(10L, 100L, "답변입니다");

    answer.update("수정된 답변");

    assertThat(answer.getContent()).isEqualTo("수정된 답변");
  }

  @Test
  @DisplayName("작성자 확인: adminId 가 같을 때만 true")
  void isWrittenBy() {
    Answer answer = Answer.create(10L, 100L, "답변");

    assertThat(answer.isWrittenBy(100L)).isTrue();
    assertThat(answer.isWrittenBy(200L)).isFalse();
  }
}
