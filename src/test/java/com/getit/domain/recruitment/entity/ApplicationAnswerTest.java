package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationAnswerTest {

  @Test
  @DisplayName("주관식 답변은 answerText 만 가진다")
  void createsTextAnswer() {
    ApplicationAnswer answer = ApplicationAnswer.create(1L, 10L, "지원 동기입니다.", null);

    assertThat(answer.getApplicationId()).isEqualTo(1L);
    assertThat(answer.getQuestionId()).isEqualTo(10L);
    assertThat(answer.getAnswerText()).isEqualTo("지원 동기입니다.");
    assertThat(answer.getSelectedOptions()).isNull();
  }

  @Test
  @DisplayName("체크박스 답변은 selectedOptions 만 가진다")
  void createsCheckboxAnswer() {
    ApplicationAnswer answer = ApplicationAnswer.create(1L, 30L, null, List.of("sw", "startup"));

    assertThat(answer.getAnswerText()).isNull();
    assertThat(answer.getSelectedOptions()).containsExactly("sw", "startup");
  }
}
