package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import com.getit.domain.recruitment.entity.EvaluationScore;
import com.getit.global.config.JpaAuditingConfig;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class EvaluationScoreRepositoryTest {

  private static final Long EVALUATOR_A = 101L;
  private static final Long EVALUATOR_B = 102L;

  @Autowired
  private EvaluationScoreRepository evaluationScoreRepository;

  @Test
  @DisplayName("지원서 id 로 점수 전체를 조회한다")
  void findsByApplicationId() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_A, 18));
    evaluationScoreRepository.save(EvaluationScore.create(1L, 20L, EVALUATOR_A, 25));
    evaluationScoreRepository.save(EvaluationScore.create(2L, 10L, EVALUATOR_A, 15));

    assertThat(evaluationScoreRepository.findByApplicationId(1L))
        .extracting(EvaluationScore::getCriterionId)
        .containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  @DisplayName("점수가 없는 지원서는 빈 리스트를 반환한다")
  void returnsEmptyWhenNoScores() {
    assertThat(evaluationScoreRepository.findByApplicationId(999L)).isEmpty();
  }

  @Test
  @DisplayName("지원서 · 기준 · 평가자가 모두 일치해야 조회된다")
  void findsOnlyWhenAllMatch() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_A, 18));

    assertThat(evaluationScoreRepository
        .findByApplicationIdAndCriterionIdAndEvaluatorId(1L, 10L, EVALUATOR_A)).isPresent();
    assertThat(evaluationScoreRepository
        .findByApplicationIdAndCriterionIdAndEvaluatorId(1L, 10L, EVALUATOR_B)).isEmpty();
    assertThat(evaluationScoreRepository
        .findByApplicationIdAndCriterionIdAndEvaluatorId(1L, 999L, EVALUATOR_A)).isEmpty();
  }

  @Test
  @DisplayName("평가자가 다르면 같은 지원서 · 기준에도 점수를 따로 남긴다")
  void allowsDifferentEvaluatorsOnSameCriterion() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_A, 18));

    // 예전 유니크 제약((지원서, 기준))에서는 여기서 터졌고, 서비스가 덮어써서
    // 먼저 매긴 사람의 점수가 사라졌다 (이슈 #151).
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_B, 25));

    assertThat(evaluationScoreRepository.findByApplicationId(1L))
        .extracting(EvaluationScore::getScore)
        .containsExactlyInAnyOrder(18, 25);
  }

  @Test
  @DisplayName("같은 평가자가 같은 기준을 두 번 저장하면 유니크 제약 위반이다")
  void throwsOnDuplicateEvaluatorScore() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_A, 18));

    assertThatThrownBy(() ->
        evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, EVALUATOR_A, 20)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
