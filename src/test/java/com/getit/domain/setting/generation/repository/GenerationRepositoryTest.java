package com.getit.domain.setting.generation.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.entity.Generation;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GenerationRepositoryTest {

  @Autowired
  private GenerationRepository generationRepository;

  @Test
  @DisplayName("활성 기수만 조회한다")
  void findsOnlyActiveGeneration() {
    generationRepository.save(Generation.create(8, 2025));
    Generation active = Generation.create(9, 2026);
    active.activate();
    generationRepository.save(active);

    assertThat(generationRepository.findByIsActiveTrue())
        .isPresent()
        .get()
        .extracting(Generation::getGenerationNo)
        .isEqualTo(9);
  }

  @Test
  @DisplayName("활성 기수가 없으면 빈 Optional 을 반환한다")
  void returnsEmptyWhenNoActiveGeneration() {
    generationRepository.save(Generation.create(8, 2025));

    assertThat(generationRepository.findByIsActiveTrue()).isEmpty();
  }

  @Test
  @DisplayName("기수 번호로 조회하고 존재 여부를 확인한다")
  void findsByGenerationNo() {
    generationRepository.save(Generation.create(9, 2026));

    assertThat(generationRepository.findByGenerationNo(9)).isPresent();
    assertThat(generationRepository.existsByGenerationNo(9)).isTrue();
    assertThat(generationRepository.existsByGenerationNo(99)).isFalse();
  }

  @Test
  @DisplayName("기수 번호가 중복되면 저장에 실패한다")
  void rejectsDuplicateGenerationNo() {
    generationRepository.saveAndFlush(Generation.create(9, 2026));

    assertThatThrownBy(() -> generationRepository.saveAndFlush(Generation.create(9, 2027)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("활성 상태인 기수만 원자적으로 비활성화하고 반영된 행 수를 반환한다")
  void deactivatesOnlyIfStillActive() {
    Generation active = Generation.create(9, 2026);
    active.activate();
    generationRepository.saveAndFlush(active);

    int updated = generationRepository.deactivateIfActive(active.getId());
    // 이미 비활성화된 뒤 다시 시도하면 0행이 반영된다 (동시 요청이 먼저 처리한 상황을 흉내낸다).
    int updatedAgain = generationRepository.deactivateIfActive(active.getId());

    assertThat(updated).isEqualTo(1);
    assertThat(updatedAgain).isEqualTo(0);
    assertThat(generationRepository.findById(active.getId()).orElseThrow().isActive()).isFalse();
  }
}
