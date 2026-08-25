package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.entity.EvaluationScore;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class EvaluationScoreRepositoryTest {

  @Autowired
  private EvaluationScoreRepository evaluationScoreRepository;

  @Test
  @DisplayName("지원서 id 로 점수 전체를 조회한다")
  void findsByApplicationId() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, 18));
    evaluationScoreRepository.save(EvaluationScore.create(1L, 20L, 25));
    evaluationScoreRepository.save(EvaluationScore.create(2L, 10L, 15));

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
  @DisplayName("applicationId 와 criterionId 가 모두 일치해야 조회된다")
  void findsByApplicationIdAndCriterionIdOnlyWhenBothMatch() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, 18));

    assertThat(evaluationScoreRepository.findByApplicationIdAndCriterionId(1L, 10L)).isPresent();
    assertThat(evaluationScoreRepository.findByApplicationIdAndCriterionId(1L, 999L)).isEmpty();
    assertThat(evaluationScoreRepository.findByApplicationIdAndCriterionId(999L, 10L)).isEmpty();
  }

  @Test
  @DisplayName("같은 (applicationId, criterionId) 로 두 번 저장하면 유니크 제약 위반이다")
  void throwsOnDuplicateApplicationAndCriterion() {
    evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, 18));

    assertThatThrownBy(() -> evaluationScoreRepository.save(EvaluationScore.create(1L, 10L, 20)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
