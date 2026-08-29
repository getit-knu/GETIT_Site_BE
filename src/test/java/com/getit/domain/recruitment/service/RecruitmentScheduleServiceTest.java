package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.RecruitmentScheduleResult;
import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RecruitmentScheduleServiceTest {

  @Autowired
  private RecruitmentScheduleService recruitmentScheduleService;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private LocalDateTime dt(int month, int day) {
    return LocalDateTime.of(2026, month, day, 0, 0);
  }

  private ScheduleUpdateCommand cmd(
      LocalDateTime totalStartAt, LocalDateTime totalEndAt,
      LocalDateTime documentStartAt, LocalDateTime documentEndAt, LocalDateTime interviewStartAt
  ) {
    return new ScheduleUpdateCommand(totalStartAt, totalEndAt, documentStartAt, documentEndAt, interviewStartAt);
  }

  @Nested
  @DisplayName("getSchedule")
  class GetSchedule {

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> recruitmentScheduleService.getSchedule())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수는 있지만 일정이 없으면 예외가 발생한다")
    void throwsWhenScheduleNotFound() {
      assertThatThrownBy(() -> recruitmentScheduleService.getSchedule())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.SCHEDULE_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수의 일정을 조회한다")
    void returnsSchedule() {
      recruitmentScheduleRepository.save(RecruitmentSchedule.create(
          activeGeneration.getId(),
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));

      RecruitmentScheduleResult found = recruitmentScheduleService.getSchedule();

      assertThat(found.generationId()).isEqualTo(activeGeneration.getId());
      assertThat(found.generationNo()).isEqualTo(activeGeneration.getGenerationNo());
      assertThat(found.year()).isEqualTo(activeGeneration.getYear());
    }
  }

  @Nested
  @DisplayName("updateSchedule")
  class UpdateSchedule {

    @Test
    @DisplayName("일정이 없으면 새로 생성한다")
    void createsWhenNotExists() {
      RecruitmentScheduleResult saved = recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));

      assertThat(saved.generationId()).isEqualTo(activeGeneration.getId());
      assertThat(saved.interviewEndAt()).isEqualTo(dt(9, 30));
    }

    @Test
    @DisplayName("일정이 있으면 덮어쓰고 interviewEndAt 을 재동기화한다")
    void updatesExisting() {
      recruitmentScheduleRepository.save(RecruitmentSchedule.create(
          activeGeneration.getId(),
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));

      RecruitmentScheduleResult updated = recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 5), dt(10, 15), dt(9, 5), dt(9, 15), dt(9, 20)));

      assertThat(updated.totalEndAt()).isEqualTo(dt(10, 15));
      assertThat(updated.interviewEndAt()).isEqualTo(dt(10, 15));
      assertThat(recruitmentScheduleRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("총 모집 시작일이 종료일보다 늦으면 검증 실패한다")
    void rejectsInvalidTotalPeriod() {
      assertThatThrownBy(() -> recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 30), dt(9, 1), dt(9, 1), dt(9, 10), dt(9, 15))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("서류 마감일이 총 모집 종료일보다 늦으면 검증 실패한다")
    void rejectsDocumentPeriodOutOfTotalPeriod() {
      assertThatThrownBy(() -> recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 1), dt(9, 10), dt(9, 1), dt(9, 20), dt(9, 25))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("면접 시작일이 서류 마감일보다 이르면 검증 실패한다")
    void rejectsInterviewBeforeDocumentEnd() {
      assertThatThrownBy(() -> recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 20), dt(9, 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("면접 시작일이 총 모집 종료일보다 늦으면 검증 실패한다")
    void rejectsInterviewStartAfterTotalEnd() {
      // interviewEndAt 은 totalEndAt(9/30) 으로 강제 동기화된다.
      // interviewStartAt(10/5) 이 이보다 늦으면 종료일 < 시작일인 깨진 일정이 된다.
      assertThatThrownBy(() -> recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(10, 5))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("활성 기수가 없으면 검증 이후 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> recruitmentScheduleService.updateSchedule(cmd(
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("updateSchedule(activeGeneration, command) 는 호출자가 이미 조회해둔 활성 기수로 저장한다")
    void updatesUsingGivenActiveGeneration() {
      GenerationSummary givenGeneration = new GenerationSummary(
          activeGeneration.getId(), activeGeneration.getGenerationNo(), activeGeneration.getYear());

      RecruitmentScheduleResult saved = recruitmentScheduleService.updateSchedule(
          givenGeneration, cmd(dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));

      assertThat(saved.generationId()).isEqualTo(activeGeneration.getId());
      assertThat(recruitmentScheduleRepository.findByGenerationId(activeGeneration.getId())).isPresent();
    }
  }
}
