package com.getit.domain.setting.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.curriculum.dto.CurriculumRequest;
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

  private CurriculumRequest request(String title, String subtitle, Integer order) {
    return new CurriculumRequest(activeGeneration.getId(), title, subtitle, order);
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
    @DisplayName("첫 커리큘럼을 추가하면 order 는 1이다")
    void createsFirstCurriculum() {
      CurriculumResult saved = curriculumAdminService.createCurriculum(
          request("팀 프로젝트", "실전 금융 IT 프로젝트 경험", 1));

      assertThat(saved.title()).isEqualTo("팀 프로젝트");
      assertThat(saved.order()).isEqualTo(1);
    }

    @Test
    @DisplayName("요청 order 가 기존 개수+1 보다 크면 맨 뒤로 clamp 된다")
    void clampsOrderAboveValidRange() {
      curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));

      CurriculumResult saved = curriculumAdminService.createCurriculum(request("B", "B", 99));

      assertThat(saved.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("중간에 끼워 넣으면 그 뒤 항목들의 순서를 한 칸씩 민다")
    void shiftsExistingItemsWhenInsertingInMiddle() {
      Curriculum first = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));
      Curriculum second = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "B", "B"));

      CurriculumResult saved = curriculumAdminService.createCurriculum(request("C", "C", 1));

      assertThat(saved.order()).isEqualTo(1);
      assertThat(curriculumRepository.findById(first.getId()).orElseThrow().getOrder()).isEqualTo(2);
      assertThat(curriculumRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("요청 generationId 가 활성 기수와 다르면 예외가 발생한다")
    void throwsWhenGenerationMismatch() {
      CurriculumRequest mismatched = new CurriculumRequest(999L, "팀 프로젝트", "실전 금융 IT 프로젝트 경험", 1);

      assertThatThrownBy(() -> curriculumAdminService.createCurriculum(mismatched))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateCurriculum")
  class UpdateCurriculum {

    @Test
    @DisplayName("내용만 바꾸면 순서는 그대로다")
    void updatesContentWithoutChangingOrder() {
      Curriculum curriculum = curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      CurriculumResult updated = curriculumAdminService.updateCurriculum(
          curriculum.getId(), request("웹 개발", "React, Node.js", 1));

      assertThat(updated.title()).isEqualTo("웹 개발");
      assertThat(updated.order()).isEqualTo(1);
    }

    @Test
    @DisplayName("앞으로 옮기면 그 사이 항목들이 한 칸씩 뒤로 밀린다")
    void movesUpAndShiftsBetweenItemsBack() {
      Curriculum first = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));
      Curriculum second = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "B", "B"));
      Curriculum third = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 3, "C", "C"));

      curriculumAdminService.updateCurriculum(third.getId(), request("C", "C", 1));

      assertThat(curriculumRepository.findById(third.getId()).orElseThrow().getOrder()).isEqualTo(1);
      assertThat(curriculumRepository.findById(first.getId()).orElseThrow().getOrder()).isEqualTo(2);
      assertThat(curriculumRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("뒤로 옮기면 그 사이 항목들이 한 칸씩 앞으로 당겨진다")
    void movesDownAndShiftsBetweenItemsForward() {
      Curriculum first = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));
      Curriculum second = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "B", "B"));
      Curriculum third = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 3, "C", "C"));

      curriculumAdminService.updateCurriculum(first.getId(), request("A", "A", 3));

      assertThat(curriculumRepository.findById(first.getId()).orElseThrow().getOrder()).isEqualTo(3);
      assertThat(curriculumRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(1);
      assertThat(curriculumRepository.findById(third.getId()).orElseThrow().getOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("요청 order 가 전체 개수보다 크면 마지막 순번으로 clamp 된다")
    void clampsOrderAboveValidRange() {
      Curriculum first = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));
      curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "B", "B"));

      CurriculumResult updated = curriculumAdminService.updateCurriculum(first.getId(), request("A", "A", 99));

      assertThat(updated.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 기수의 커리큘럼이면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Curriculum other = curriculumRepository.save(
          Curriculum.create(otherGeneration.getId(), 1, "지난 기수 커리큘럼", "지난 기수"));

      assertThatThrownBy(() -> curriculumAdminService.updateCurriculum(
          other.getId(), request("웹 개발", "React, Node.js", 1)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 커리큘럼이면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> curriculumAdminService.updateCurriculum(
          999L, request("웹 개발", "React, Node.js", 1)))
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
    @DisplayName("삭제하고 뒤 순번을 한 칸씩 당긴다")
    void deletesAndShiftsRemainingOrder() {
      Curriculum first = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 1, "A", "A"));
      Curriculum second = curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "B", "B"));

      curriculumAdminService.deleteCurriculum(first.getId());

      assertThat(curriculumRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(1);
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
