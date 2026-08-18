package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluationCriterionTest {

  @Test
  @DisplayName("평가 기준을 생성한다")
  void createsCriterion() {
    EvaluationCriterion criterion = EvaluationCriterion.create(
        1L, 1, "전공 적합성", "전공 적합성 가이드 라인", 20);

    assertThat(criterion.getGenerationId()).isEqualTo(1L);
    assertThat(criterion.getOrder()).isEqualTo(1);
    assertThat(criterion.getName()).isEqualTo("전공 적합성");
    assertThat(criterion.getGuideline()).isEqualTo("전공 적합성 가이드 라인");
    assertThat(criterion.getMaxScore()).isEqualTo(20);
  }

  @Test
  @DisplayName("수정 시 order · generationId 는 바뀌지 않는다")
  void updateKeepsOrderAndGenerationId() {
    EvaluationCriterion criterion = EvaluationCriterion.create(
        1L, 1, "전공 적합성", "원래 가이드 라인", 20);

    criterion.update("전공 적합성(수정)", "수정된 가이드 라인", 25);

    assertThat(criterion.getGenerationId()).isEqualTo(1L);
    assertThat(criterion.getOrder()).isEqualTo(1);
    assertThat(criterion.getName()).isEqualTo("전공 적합성(수정)");
    assertThat(criterion.getGuideline()).isEqualTo("수정된 가이드 라인");
    assertThat(criterion.getMaxScore()).isEqualTo(25);
  }

  @Test
  @DisplayName("순서만 변경한다")
  void updatesOrderOnly() {
    EvaluationCriterion criterion = EvaluationCriterion.create(
        1L, 1, "전공 적합성", "가이드 라인", 20);

    criterion.updateOrder(3);

    assertThat(criterion.getOrder()).isEqualTo(3);
    assertThat(criterion.getName()).isEqualTo("전공 적합성");
  }
}
