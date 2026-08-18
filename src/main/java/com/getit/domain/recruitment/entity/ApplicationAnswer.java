package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지원서 답변. (API 명세서 3.2 · 3.3)
 *
 * <p>{@code (applicationId, questionId)} 로 유일하다 — 질문 하나에 답변 하나다.
 * {@code answerText} 는 TEXT 질문, {@code selectedOptions} 는 CHOICE · CHECKBOX 질문에 쓰며
 * 응답한 질문 유형이 아닌 쪽은 null 이다. 저장 시 upsert 로직은 3.3 에서 추가한다.
 */
@Entity
@Table(name = "application_answer", uniqueConstraints = {
    @UniqueConstraint(name = "uk_application_answer_question", columnNames = {"application_id", "question_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationAnswer extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long applicationId;

  @Column(nullable = false)
  private Long questionId;

  @Column(columnDefinition = "TEXT")
  private String answerText;

  @Convert(converter = SelectedOptionsConverter.class)
  @Column(columnDefinition = "json")
  private List<String> selectedOptions;

  @Builder(access = AccessLevel.PRIVATE)
  private ApplicationAnswer(
      Long applicationId,
      Long questionId,
      String answerText,
      List<String> selectedOptions
  ) {
    this.applicationId = applicationId;
    this.questionId = questionId;
    this.answerText = answerText;
    this.selectedOptions = selectedOptions;
  }

  public static ApplicationAnswer create(
      Long applicationId,
      Long questionId,
      String answerText,
      List<String> selectedOptions
  ) {
    return ApplicationAnswer.builder()
        .applicationId(applicationId)
        .questionId(questionId)
        .answerText(answerText)
        .selectedOptions(selectedOptions)
        .build();
  }
}
