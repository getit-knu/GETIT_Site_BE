package com.getit.domain.recruitment.entity;

import com.getit.domain.recruitment.dto.QuestionOption;
import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 지원서 질문 항목. (API 명세서 6.3 · 6.4 · 6.5 · 6.6 · 6.7)
 *
 * <p>options 는 CHOICE · CHECKBOX 일 때만 값이 있다. 저장 시 유효성(2개 이상 등)은
 * 서비스 레이어에서 검증한다 — 이 엔티티는 검증 없이 그대로 담는다 ({@code RecruitmentSchedule} 과 동일 원칙).
 */
@Entity
@Table(name = "application_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationQuestion extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generationId;

  /** order 는 SQL 예약어라 컬럼명을 분리한다. */
  @Column(name = "question_order", nullable = false)
  private Integer order;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private QuestionType type;

  @Column(nullable = false, length = 500)
  private String content;

  @Column(nullable = false)
  private boolean required;

  /** TEXT 타입에서만 쓴다. 기본값 300 은 서비스에서 채운다. */
  @Column
  private Integer maxLength;

  @Convert(converter = QuestionOptionListConverter.class)
  @Column(columnDefinition = "json")
  private List<QuestionOption> options;

  @Builder(access = AccessLevel.PRIVATE)
  private ApplicationQuestion(
      Long generationId,
      Integer order,
      QuestionType type,
      String content,
      boolean required,
      Integer maxLength,
      List<QuestionOption> options
  ) {
    this.generationId = generationId;
    this.order = order;
    this.type = type;
    this.content = content;
    this.required = required;
    this.maxLength = maxLength;
    this.options = options;
  }

  public static ApplicationQuestion create(
      Long generationId,
      Integer order,
      QuestionType type,
      String content,
      boolean required,
      Integer maxLength,
      List<QuestionOption> options
  ) {
    return ApplicationQuestion.builder()
        .generationId(generationId)
        .order(order)
        .type(type)
        .content(content)
        .required(required)
        .maxLength(maxLength)
        .options(options)
        .build();
  }

  /** 6.5 PUT. order · generationId 는 바꾸지 않는다 (전용 API 는 6.7). */
  public void update(
      QuestionType type,
      String content,
      boolean required,
      Integer maxLength,
      List<QuestionOption> options
  ) {
    this.type = type;
    this.content = content;
    this.required = required;
    this.maxLength = maxLength;
    this.options = options;
  }

  /** 6.7 PUT /order. */
  public void updateOrder(int order) {
    this.order = order;
  }
}
