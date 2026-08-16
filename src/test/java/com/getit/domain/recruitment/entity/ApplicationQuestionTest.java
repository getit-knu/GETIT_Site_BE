package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.dto.QuestionOption;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationQuestionTest {

  @Test
  @DisplayName("주관식 질문을 생성한다")
  void createsTextQuestion() {
    ApplicationQuestion question = ApplicationQuestion.create(
        1L, 1, QuestionType.TEXT, "지원 동기를 작성해주세요", true, 300, null);

    assertThat(question.getGenerationId()).isEqualTo(1L);
    assertThat(question.getOrder()).isEqualTo(1);
    assertThat(question.getType()).isEqualTo(QuestionType.TEXT);
    assertThat(question.getContent()).isEqualTo("지원 동기를 작성해주세요");
    assertThat(question.isRequired()).isTrue();
    assertThat(question.getMaxLength()).isEqualTo(300);
    assertThat(question.getOptions()).isNull();
  }

  @Test
  @DisplayName("체크박스 질문은 options 를 가진다")
  void createsCheckboxQuestion() {
    List<QuestionOption> options = List.of(
        new QuestionOption("sw", "SW 개발"),
        new QuestionOption("startup", "창업"));

    ApplicationQuestion question = ApplicationQuestion.create(
        1L, 3, QuestionType.CHECKBOX, "관심 있는 트랙을 선택해주세요", true, null, options);

    assertThat(question.getOptions()).containsExactly(
        new QuestionOption("sw", "SW 개발"),
        new QuestionOption("startup", "창업"));
  }

  @Test
  @DisplayName("수정 시 order · generationId 는 바뀌지 않는다")
  void updateKeepsOrderAndGenerationId() {
    ApplicationQuestion question = ApplicationQuestion.create(
        1L, 1, QuestionType.TEXT, "원래 내용", false, 300, null);

    question.update(QuestionType.TEXT, "수정된 내용", true, 200, null);

    assertThat(question.getGenerationId()).isEqualTo(1L);
    assertThat(question.getOrder()).isEqualTo(1);
    assertThat(question.getContent()).isEqualTo("수정된 내용");
    assertThat(question.isRequired()).isTrue();
    assertThat(question.getMaxLength()).isEqualTo(200);
  }

  @Test
  @DisplayName("순서만 변경한다")
  void updatesOrderOnly() {
    ApplicationQuestion question = ApplicationQuestion.create(
        1L, 1, QuestionType.TEXT, "내용", false, 300, null);

    question.updateOrder(3);

    assertThat(question.getOrder()).isEqualTo(3);
    assertThat(question.getContent()).isEqualTo("내용");
  }
}
