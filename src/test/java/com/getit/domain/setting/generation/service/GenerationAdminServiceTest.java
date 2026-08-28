package com.getit.domain.setting.generation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.dto.GenerationResult;
import com.getit.domain.setting.generation.dto.UpdateGenerationCommand;
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
      GenerationResult saved = generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));

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

      generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));

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

      GenerationResult saved = generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));

      assertThat(saved.year()).isEqualTo(2026);
      assertThat(saved.isActive()).isTrue();
      // 활성화 로직을 직렬화하는 예약 행(generationNo=0)이 하나 더 생기므로, 9기가 중복
      // 생성되지 않았는지는 "2건(예약 행 + 9기)"으로 확인한다.
      assertThat(generationRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("현재 활성 기수와 같은 기수 번호면 비활성화 없이 정보만 갱신한다")
    void updatesSameActiveGenerationWithoutDeactivating() {
      Generation active = Generation.create(9, 2025);
      active.activate();
      generationRepository.save(active);

      GenerationResult saved = generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));

      assertThat(saved.year()).isEqualTo(2026);
      assertThat(saved.isActive()).isTrue();
    }

    @Test
    @DisplayName("활성화 로직을 직렬화하는 예약 행(generationNo=0)을 만들지만 조회 결과에는 나타나지 않는다")
    void createsActivationLockRowButExcludesItFromResults() {
      generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));

      assertThat(generationRepository.findByGenerationNo(0)).isPresent();
      GenerationResult active = generationAdminService.getActiveGeneration();
      assertThat(active.generationNo()).isEqualTo(9);
    }

    @Test
    @DisplayName("예약 행이 이미 있어도(두 번째 호출) 중복 생성 없이 정상 동작한다")
    void reusesExistingActivationLockRowOnSubsequentCalls() {
      generationAdminService.updateGeneration(new UpdateGenerationCommand(9, 2026));
      long lockRowCountAfterFirstCall = generationRepository.count();

      GenerationResult saved = generationAdminService.updateGeneration(new UpdateGenerationCommand(10, 2027));

      assertThat(saved.generationNo()).isEqualTo(10);
      // 예약 행(1) + 9기(1) 에서 10기(1) 하나만 늘어야 한다 — 예약 행이 중복 생성되면 안 된다.
      assertThat(generationRepository.count()).isEqualTo(lockRowCountAfterFirstCall + 1);
    }
  }
}
