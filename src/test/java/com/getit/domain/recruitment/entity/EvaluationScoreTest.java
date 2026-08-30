package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EvaluationScoreTest {

  @Test
  @DisplayName("생성하면 applicationId · criterionId · score 가 그대로 담긴다")
  void createHoldsFields() {
    EvaluationScore score = EvaluationScore.create(1L, 10L, 101L, 18);

    assertThat(score.getApplicationId()).isEqualTo(1L);
    assertThat(score.getCriterionId()).isEqualTo(10L);
    assertThat(score.getScore()).isEqualTo(18);
  }

  @Test
  @DisplayName("updateScore 는 점수만 덮어쓴다")
  void updateScoreOverwritesScoreOnly() {
    EvaluationScore score = EvaluationScore.create(1L, 10L, 101L, 18);

    score.updateScore(20);

    assertThat(score.getApplicationId()).isEqualTo(1L);
    assertThat(score.getCriterionId()).isEqualTo(10L);
    assertThat(score.getScore()).isEqualTo(20);
  }
}
