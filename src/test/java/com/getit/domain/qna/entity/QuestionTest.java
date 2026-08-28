package com.getit.domain.qna.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestionTest {

  @Test
  @DisplayName("생성: 상태는 PENDING 이다")
  void creates() {
    Question question = Question.create(1L, 5L, "질문입니다");

    assertThat(question.getStatus()).isEqualTo(QnaStatus.PENDING);
    assertThat(question.getAuthorId()).isEqualTo(1L);
    assertThat(question.getLectureId()).isEqualTo(5L);
    assertThat(question.getContent()).isEqualTo("질문입니다");
  }

  @Test
  @DisplayName("사이트 Q&A: lectureId 는 null 이다")
  void createsSiteQuestion() {
    Question question = Question.create(1L, null, "질문");

    assertThat(question.getLectureId()).isNull();
  }

  @Test
  @DisplayName("답변 등록: 상태가 ANSWERED 로 바뀐다")
  void markAnswered() {
    Question question = Question.create(1L, 5L, "질문");

    question.markAnswered();

    assertThat(question.getStatus()).isEqualTo(QnaStatus.ANSWERED);
  }
}
