package com.getit.domain.setting.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.dto.GenerationResult;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.exception.GenerationErrorCode;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GenerationAdminServiceTest {

  @Autowired
  private GenerationAdminService generationAdminService;

  @Autowired
  private GenerationRepository generationRepository;

  @Nested
  @DisplayName("getActiveGeneration")
  class GetActiveGeneration {

    @Test
    @DisplayName("활성 기수를 조회한다")
    void returnsActiveGeneration() {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generationRepository.save(generation);

      GenerationResult found = generationAdminService.getActiveGeneration();

      assertThat(found.generationNo()).isEqualTo(9);
      assertThat(found.year()).isEqualTo(2026);
      assertThat(found.isActive()).isTrue();
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      generationRepository.save(Generation.create(8, 2025));

      assertThatThrownBy(() -> generationAdminService.getActiveGeneration())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(GenerationErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateGeneration")
  class UpdateGeneration {

    @Test
    @DisplayName("활성 기수가 하나도 없으면 새로 만들어서 활성화한다")
    void createsAndActivatesWhenNoneExists() {
      GenerationResult saved = generationAdminService.updateGeneration(9, 2026);

      assertThat(saved.generationNo()).isEqualTo(9);
      assertThat(saved.year()).isEqualTo(2026);
      assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("새 기수를 활성화하면 기존 활성 기수는 비활성화된다")
    void deactivatesPreviousActiveGeneration() {
      Generation previous = Generation.create(8, 2025);
      previous.activate();
      generationRepository.save(previous);

      generationAdminService.updateGeneration(9, 2026);

      assertThat(generationRepository.findById(previous.getId()).orElseThrow().isActive()).isFalse();
      assertThat(generationRepository.findByIsActiveTrue())
          .isPresent()
          .get()
          .extracting(Generation::getGenerationNo)
          .isEqualTo(9);
    }

    @Test
    @DisplayName("이미 있는 기수 번호면 연도만 갱신하고 활성화한다")
    void updatesExistingGenerationInfo() {
      generationRepository.save(Generation.create(9, 2025));

      GenerationResult saved = generationAdminService.updateGeneration(9, 2026);

      assertThat(saved.year()).isEqualTo(2026);
      assertThat(saved.isActive()).isTrue();
      assertThat(generationRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("현재 활성 기수와 같은 기수 번호면 비활성화 없이 정보만 갱신한다")
    void updatesSameActiveGenerationWithoutDeactivating() {
      Generation active = Generation.create(9, 2025);
      active.activate();
      generationRepository.save(active);

      GenerationResult saved = generationAdminService.updateGeneration(9, 2026);

      assertThat(saved.year()).isEqualTo(2026);
      assertThat(saved.isActive()).isTrue();
    }
  }
}
