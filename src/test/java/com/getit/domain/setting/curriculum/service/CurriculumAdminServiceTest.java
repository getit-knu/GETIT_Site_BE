package com.getit.domain.setting.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.curriculum.dto.CurriculumResult;
import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.exception.CurriculumErrorCode;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CurriculumAdminServiceTest {

  @Autowired
  private CurriculumAdminService curriculumAdminService;

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  @Nested
  @DisplayName("getCurriculums")
  class GetCurriculums {

    @Test
    @DisplayName("활성 기수의 커리큘럼을 order 순으로 반환한다")
    void returnsCurriculumsInOrder() {
      curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "웹 개발", "React, Node.js"));
      curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));
      curriculumRepository.save(Curriculum.create(99L, 1, "다른 기수 커리큘럼", "다른 기수"));

      List<CurriculumResult> results = curriculumAdminService.getCurriculums();

      assertThat(results).extracting(CurriculumResult::title)
          .containsExactly("Python & 데이터 분석", "웹 개발");
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> curriculumAdminService.getCurriculums())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createCurriculum")
  class CreateCurriculum {

    @Test
    @DisplayName("활성 기수에 커리큘럼을 추가한다")
    void createsCurriculum() {
      CurriculumResult saved = curriculumAdminService.createCurriculum(
          activeGeneration.getId(), "팀 프로젝트", "실전 금융 IT 프로젝트 경험", 4);

      assertThat(saved.title()).isEqualTo("팀 프로젝트");
      assertThat(saved.order()).isEqualTo(4);
    }

    @Test
    @DisplayName("요청 generationId 가 활성 기수와 다르면 예외가 발생한다")
    void throwsWhenGenerationMismatch() {
      assertThatThrownBy(() -> curriculumAdminService.createCurriculum(
          999L, "팀 프로젝트", "실전 금융 IT 프로젝트 경험", 4))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateCurriculum")
  class UpdateCurriculum {

    @Test
    @DisplayName("커리큘럼을 수정한다")
    void updatesCurriculum() {
      Curriculum curriculum = curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      CurriculumResult updated = curriculumAdminService.updateCurriculum(
          curriculum.getId(), activeGeneration.getId(), "웹 개발", "React, Node.js", 2);

      assertThat(updated.title()).isEqualTo("웹 개발");
      assertThat(updated.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 기수의 커리큘럼이면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Curriculum other = curriculumRepository.save(
          Curriculum.create(otherGeneration.getId(), 1, "지난 기수 커리큘럼", "지난 기수"));

      assertThatThrownBy(() -> curriculumAdminService.updateCurriculum(
          other.getId(), activeGeneration.getId(), "웹 개발", "React, Node.js", 2))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 커리큘럼이면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> curriculumAdminService.updateCurriculum(
          999L, activeGeneration.getId(), "웹 개발", "React, Node.js", 2))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteCurriculum")
  class DeleteCurriculum {

    @Test
    @DisplayName("커리큘럼을 삭제한다")
    void deletesCurriculum() {
      Curriculum curriculum = curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      curriculumAdminService.deleteCurriculum(curriculum.getId());

      assertThat(curriculumRepository.findById(curriculum.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 기수의 커리큘럼이면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Curriculum other = curriculumRepository.save(
          Curriculum.create(otherGeneration.getId(), 1, "지난 기수 커리큘럼", "지난 기수"));

      assertThatThrownBy(() -> curriculumAdminService.deleteCurriculum(other.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
    }
  }
}
