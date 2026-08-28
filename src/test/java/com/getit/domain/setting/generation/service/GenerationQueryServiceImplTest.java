package com.getit.domain.setting.generation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 소비하는 기수 조회 계약. (작업 분할 계획 4.2, 이슈 #22) */
@SpringBootTest
@Transactional
class GenerationQueryServiceImplTest {

  @Autowired
  private GenerationQueryService generationQueryService;

  @Autowired
  private GenerationRepository generationRepository;

  @Test
  @DisplayName("활성 기수가 있으면 GenerationSummary 를 반환한다")
  void returnsActiveGeneration() {
    generationRepository.save(Generation.create(8, 2025));
    Generation active = Generation.create(9, 2026);
    active.activate();
    generationRepository.save(active);

    GenerationSummary summary = generationQueryService.findActive().orElseThrow();

    assertThat(summary.id()).isEqualTo(active.getId());
    assertThat(summary.generationNo()).isEqualTo(9);
    assertThat(summary.year()).isEqualTo(2026);
  }

  @Test
  @DisplayName("활성 기수가 없으면 빈 Optional 을 반환한다")
  void returnsEmptyWhenNoActiveGeneration() {
    generationRepository.save(Generation.create(8, 2025));

    assertThat(generationQueryService.findActive()).isEmpty();
  }

  @Test
  @DisplayName("id 로 기수를 조회한다")
  void returnsGenerationById() {
    Generation generation = generationRepository.save(Generation.create(9, 2026));

    GenerationSummary summary = generationQueryService.findById(generation.getId()).orElseThrow();

    assertThat(summary.generationNo()).isEqualTo(9);
    assertThat(summary.year()).isEqualTo(2026);
  }

  @Test
  @DisplayName("없는 id 로 조회하면 빈 Optional 을 반환한다")
  void returnsEmptyForUnknownId() {
    assertThat(generationQueryService.findById(999_999L)).isEmpty();
  }

  @Test
  @DisplayName("기수 번호로 조회한다")
  void returnsGenerationByGenerationNo() {
    generationRepository.save(Generation.create(9, 2026));

    GenerationSummary summary = generationQueryService.findByGenerationNo(9).orElseThrow();

    assertThat(summary.generationNo()).isEqualTo(9);
    assertThat(summary.year()).isEqualTo(2026);
  }

  @Test
  @DisplayName("없는 기수 번호로 조회하면 빈 Optional 을 반환한다")
  void returnsEmptyForUnknownGenerationNo() {
    assertThat(generationQueryService.findByGenerationNo(999)).isEmpty();
  }

  @Test
  @DisplayName("활성화 로직 잠금용 예약 기수 번호는 행이 있어도 빈 Optional 을 반환한다")
  void returnsEmptyForReservedActivationLockGenerationNo() {
    generationRepository.save(Generation.create(Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO, 0));

    assertThat(generationQueryService.findByGenerationNo(Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO))
        .isEmpty();
  }
}
