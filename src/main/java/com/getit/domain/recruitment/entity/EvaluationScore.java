package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서류 평가 점수. (API 명세서 7.3)
 *
 * <p>지원서(applicationId) 하나에 평가 기준(criterionId) 하나당 점수 하나가 달린다. 배점(maxScore)을
 * 넘는지는 서비스 레이어에서 검증한다 — 이 엔티티는 검증 없이 그대로 담는다 ({@code EvaluationCriterion}
 * 과 동일 원칙).
 */
@Entity
@Table(name = "evaluation_score", uniqueConstraints = {
    @UniqueConstraint(name = "uk_evaluation_score_app_criterion_evaluator",
        columnNames = {"application_id", "criterion_id", "evaluator_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EvaluationScore extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long applicationId;

  @Column(nullable = false)
  private Long criterionId;

  /** 이 점수를 매긴 운영진. 평가자마다 자기 점수를 따로 갖는다. */
  @Column(nullable = false)
  private Long evaluatorId;

  @Column(nullable = false)
  private Integer score;

  @Builder(access = AccessLevel.PRIVATE)
  private EvaluationScore(Long applicationId, Long criterionId, Long evaluatorId, Integer score) {
    this.applicationId = applicationId;
    this.criterionId = criterionId;
    this.evaluatorId = evaluatorId;
    this.score = score;
  }

  public static EvaluationScore create(
      Long applicationId, Long criterionId, Long evaluatorId, Integer score) {
    return EvaluationScore.builder()
        .applicationId(applicationId)
        .criterionId(criterionId)
        .evaluatorId(evaluatorId)
        .score(score)
        .build();
  }

  /** 7.3 upsert 시 이미 저장된 점수를 덮어쓸 때 쓴다. */
  public void updateScore(Integer score) {
    this.score = score;
  }
}
