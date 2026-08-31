package com.getit.domain.setting.generation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

  /**
   * 확인만 하고 잠그지 않으면, 확인 직후 다른 트랜잭션이 기수를 전환해도 그 결과에 기대어
   * 한 쓰기가 그대로 커밋된다 (PR #169 리뷰 지적). 잠금 자체는 DB 동시성이라 여기서
   * 재현하지 않는다 — 여기서는 결과가 findActive 와 어긋나지 않는지만 본다.
   */
  @Nested
  @DisplayName("findActiveForWrite")
  class FindActiveForWrite {

    @Test
    @DisplayName("활성 기수를 findActive 와 같게 반환한다")
    void returnsSameAsFindActive() {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generationRepository.save(generation);

      assertThat(generationQueryService.findActiveForWrite())
          .isEqualTo(generationQueryService.findActive());
      assertThat(generationQueryService.findActiveForWrite().orElseThrow().generationNo())
          .isEqualTo(9);
    }

    @Test
    @DisplayName("활성 기수가 없으면 빈 Optional 을 반환한다")
    void returnsEmptyWhenNoActiveGeneration() {
      generationRepository.save(Generation.create(9, 2026));

      assertThat(generationQueryService.findActiveForWrite()).isEmpty();
    }

    @Test
    @DisplayName("잠금 행이 아직 없어도 (최초 활성화 전) 실패하지 않는다")
    void worksBeforeTheActivationLockRowExists() {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generationRepository.save(generation);

      // 잠금 행은 GenerationAdminService 가 최초 활성화 때 만든다. 그전에도 터지면 안 된다.
      assertThat(generationRepository.findByGenerationNo(
          Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO)).isEmpty();
      assertThat(generationQueryService.findActiveForWrite()).isPresent();
    }
  }
}
