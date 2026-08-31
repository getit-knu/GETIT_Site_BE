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
 * 응답한 질문 유형이 아닌 쪽은 null 이다. upsert(3.3)는 서비스 레이어가 이 답변이 이미 있는지
 * 조회해서 있으면 {@link #update}, 없으면 {@link #create} 를 호출하는 방식으로 한다.
 */
@Entity
@Table(name = "application_answer", uniqueConstraints = {
    @UniqueConstraint(name = "uk_application_answer_question", columnNames = {"application_id", "question_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationAnswer extends BaseTimeEntity {

  /**
   * 답변 하나가 담을 수 있는 최대 글자 수. 컬럼이 감당하는 물리적 상한이다.
   *
   * <p>{@code answerText} 는 {@code TEXT}(65,535 바이트)이고 DB 문자셋은 {@code utf8mb4} 라
   * 글자당 최대 4 바이트다. 그래서 최악의 경우 16,383 자에서 넘친다. 여유를 두어 16,000 으로
   * 끊는다.
   *
   * <p>질문별 글자 수 제한({@code ApplicationQuestion.maxLength}, TEXT 기본 300 자)과는 다른
   * 층이다. 그쪽은 운영진이 정하는 정책이고 제출 시점에만 본다. 이쪽은 무엇을 설정하든
   * 넘을 수 없는 바닥이다 — 없으면 임시저장에서 그대로 컬럼에 들어가다 500 이 난다 (이슈 #171).
   */
  public static final int MAX_ANSWER_LENGTH = 16_000;

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

  /** 3.3 임시 저장 upsert. 이미 있는 답변을 덮어쓸 때 쓴다. */
  public void update(String answerText, List<String> selectedOptions) {
    this.answerText = answerText;
    this.selectedOptions = selectedOptions;
  }
}
