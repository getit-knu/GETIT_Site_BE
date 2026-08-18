package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class EvaluationCriterionRepositoryTest {

  @Autowired
  private EvaluationCriterionRepository evaluationCriterionRepository;

  private EvaluationCriterion criterion(Long generationId, int order, String name, int maxScore) {
    return EvaluationCriterion.create(generationId, order, name, name + " 가이드 라인", maxScore);
  }

  @Test
  @DisplayName("기수별로 order 오름차순 조회한다")
  void findsByGenerationIdOrderedByOrder() {
    evaluationCriterionRepository.save(criterion(1L, 2, "지원 동기", 30));
    evaluationCriterionRepository.save(criterion(1L, 1, "전공 적합성", 20));
    evaluationCriterionRepository.save(criterion(2L, 1, "다른 기수", 50));

    assertThat(evaluationCriterionRepository.findByGenerationId(1L))
        .extracting(EvaluationCriterion::getName)
        .containsExactly("전공 적합성", "지원 동기");
  }

  @Test
  @DisplayName("기준이 없는 기수는 빈 리스트를 반환한다")
  void returnsEmptyWhenNoCriteria() {
    assertThat(evaluationCriterionRepository.findByGenerationId(999L)).isEmpty();
  }

  @Test
  @DisplayName("id 와 기수가 모두 일치해야 조회된다")
  void findsByIdAndGenerationIdOnlyWhenBothMatch() {
    EvaluationCriterion saved = evaluationCriterionRepository.save(criterion(1L, 1, "전공 적합성", 20));

    assertThat(evaluationCriterionRepository.findByIdAndGenerationId(saved.getId(), 1L)).isPresent();
    assertThat(evaluationCriterionRepository.findByIdAndGenerationId(saved.getId(), 2L)).isEmpty();
  }
}
