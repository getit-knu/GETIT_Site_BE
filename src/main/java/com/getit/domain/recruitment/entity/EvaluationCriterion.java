package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서류 평가 기준. (API 명세서 6.8 · 6.9 · 6.10 · 6.11)
 *
 * <p>기수별 배점 합계가 100점을 넘는지는 서비스 레이어에서 검증한다 — 이 엔티티는 검증 없이
 * 그대로 담는다 ({@code ApplicationQuestion} 과 동일 원칙).
 */
@Entity
@Table(name = "evaluation_criterion")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationCriterion extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generationId;

  /** order 는 SQL 예약어라 컬럼명을 분리한다. */
  @Column(name = "criterion_order", nullable = false)
  private Integer order;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 500)
  private String guideline;

  @Column(nullable = false)
  private Integer maxScore;

  @Builder(access = AccessLevel.PRIVATE)
  private EvaluationCriterion(
      Long generationId,
      Integer order,
      String name,
      String guideline,
      Integer maxScore
  ) {
    this.generationId = generationId;
    this.order = order;
    this.name = name;
    this.guideline = guideline;
    this.maxScore = maxScore;
  }

  public static EvaluationCriterion create(
      Long generationId,
      Integer order,
      String name,
      String guideline,
      Integer maxScore
  ) {
    return EvaluationCriterion.builder()
        .generationId(generationId)
        .order(order)
        .name(name)
        .guideline(guideline)
        .maxScore(maxScore)
        .build();
  }

  /** 6.10 PUT. order · generationId 는 바꾸지 않는다. */
  public void update(String name, String guideline, Integer maxScore) {
    this.name = name;
    this.guideline = guideline;
    this.maxScore = maxScore;
  }

  public void updateOrder(int order) {
    this.order = order;
  }
}
