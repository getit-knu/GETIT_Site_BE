package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.dto.RecruitmentStatusResult;
import com.getit.domain.recruitment.entity.RecruitmentPhase;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code Clock} 을 고정해서 단계 경계값·D-day 를 결정적으로 검증한다 — {@code LocalDateTime
 * .now()} 를 그대로 쓰면 테스트를 실행하는 실제 시각에 따라 결과가 달라지고, 자정을 사이에
 * 두면 D-day 검증이 간헐적으로 실패할 수 있었다(PR #86 Copilot 리뷰 지적).
 */
@SpringBootTest
@Transactional
class RecruitmentStatusServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0, 0);

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
    }
  }

  @Autowired
  private RecruitmentStatusService recruitmentStatusService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  private Generation activeGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    return generationRepository.save(generation);
  }

  @Test
  @DisplayName("활성 기수가 없으면 CLOSED 다")
  void returnsClosedWhenNoActiveGeneration() {
    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.CLOSED);
    assertThat(status.dDay()).isNull();
    assertThat(status.applyEnabled()).isFalse();
    assertThat(status.schedule()).isNull();
  }

  @Test
  @DisplayName("활성 기수는 있지만 일정이 없으면 CLOSED 다")
  void returnsClosedWhenScheduleNotFound() {
    activeGeneration();

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.CLOSED);
  }

  @Test
  @DisplayName("서류 접수 기간이면 DOCUMENT_OPEN 이고 지원 가능하다")
  void returnsDocumentOpenWhenWithinDocumentPeriod() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(5), NOW.plusDays(20),
        NOW.minusDays(5), NOW.plusDays(5), NOW.plusDays(10)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.DOCUMENT_OPEN);
    assertThat(status.applyEnabled()).isTrue();
    assertThat(status.dDay()).isEqualTo(5L);
    assertThat(status.message()).isEqualTo("서류 마감까지 D-5");
    assertThat(status.generationNo()).isEqualTo(9);
    assertThat(status.schedule().documentEndAt()).isEqualTo(NOW.plusDays(5).atZone(SEOUL).toOffsetDateTime());
  }

  @Test
  @DisplayName("모집 시작 전이면 BEFORE_OPEN 이고 지원할 수 없다")
  void returnsBeforeOpenBeforeDocumentPeriod() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.plusDays(10), NOW.plusDays(40),
        NOW.plusDays(10), NOW.plusDays(20), NOW.plusDays(30)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.BEFORE_OPEN);
    assertThat(status.applyEnabled()).isFalse();
    assertThat(status.dDay()).isEqualTo(10L);
    assertThat(status.message()).isEqualTo("모집 시작까지 D-10");
  }

  @Test
  @DisplayName("서류 심사 기간이면 DOCUMENT_REVIEW 다")
  void returnsDocumentReviewAfterDocumentPeriod() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(20), NOW.plusDays(10),
        NOW.minusDays(20), NOW.minusDays(5), NOW.plusDays(5)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.DOCUMENT_REVIEW);
    assertThat(status.applyEnabled()).isFalse();
    assertThat(status.dDay()).isEqualTo(5L);
    assertThat(status.message()).isEqualTo("면접까지 D-5");
  }

  @Test
  @DisplayName("면접 기간이면 INTERVIEW 다")
  void returnsInterviewDuringInterviewPeriod() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(20), NOW.plusDays(5),
        NOW.minusDays(20), NOW.minusDays(10), NOW.minusDays(1)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.INTERVIEW);
    assertThat(status.dDay()).isEqualTo(5L);
    assertThat(status.message()).isEqualTo("최종 발표까지 D-5");
  }

  @Test
  @DisplayName("모집 종료 뒤면 FINAL_ANNOUNCED 이고 dDay 가 없다")
  void returnsFinalAnnouncedAfterTotalEnd() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(30), NOW.minusDays(1),
        NOW.minusDays(30), NOW.minusDays(20), NOW.minusDays(10)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.FINAL_ANNOUNCED);
    assertThat(status.dDay()).isNull();
    assertThat(status.message()).isEqualTo("최종 합격자가 발표되었습니다");
  }

  @Test
  @DisplayName("getStatus(activeGeneration) 는 호출자가 이미 조회해둔 활성 기수로 조회한다 — findActive() 를 다시 부르지 않는다")
  void returnsStatusUsingGivenActiveGeneration() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(5), NOW.plusDays(20),
        NOW.minusDays(5), NOW.plusDays(5), NOW.plusDays(10)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus(
        new GenerationSummary(generation.getId(), generation.getGenerationNo(), generation.getYear()));

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.DOCUMENT_OPEN);
    assertThat(status.applyEnabled()).isTrue();
  }

  @Test
  @DisplayName("getStatus(null) 은 CLOSED 다 — 활성 기수가 없다는 뜻이다")
  void returnsClosedWhenGivenGenerationIsNull() {
    RecruitmentStatusResult status = recruitmentStatusService.getStatus(null);

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.CLOSED);
  }

  /**
   * 지원 스위치는 일정과 별개다. (이슈 #170)
   *
   * <p>단계는 그대로 DOCUMENT_OPEN 이고 일정 값도 그대로 남는다. 바뀌는 것은 지원 가능
   * 여부와 안내 문구뿐이다 — 급히 멈추려고 마감일을 당기면 D-day 와 일정 표시가 망가진다.
   */
  private RecruitmentSchedule openScheduleWithToggle(boolean applyEnabled) {
    Generation generation = activeGeneration();
    RecruitmentSchedule schedule = RecruitmentSchedule.create(
        generation.getId(), NOW.minusDays(5), NOW.plusDays(20),
        NOW.minusDays(5), NOW.plusDays(5), NOW.plusDays(10));
    schedule.changeApplyEnabled(applyEnabled);
    return recruitmentScheduleRepository.save(schedule);
  }

  @Test
  @DisplayName("서류 기간이라도 스위치를 내리면 지원할 수 없다")
  void closesApplyWhileStillInDocumentPeriod() {
    openScheduleWithToggle(false);

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.applyEnabled()).isFalse();
    assertThat(status.message()).isEqualTo("지원 접수가 일시 중지되었습니다");
  }

  @Test
  @DisplayName("스위치를 내려도 단계와 일정은 그대로다")
  void keepsPhaseAndScheduleIntactWhenPaused() {
    openScheduleWithToggle(false);

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    // 마감일을 당겨서 멈추는 것과 다른 점이 바로 이것이다.
    assertThat(status.phase()).isEqualTo(RecruitmentPhase.DOCUMENT_OPEN);
    assertThat(status.dDay()).isEqualTo(5L);
    assertThat(status.schedule().documentEndAt())
        .isEqualTo(NOW.plusDays(5).atZone(SEOUL).toOffsetDateTime());
  }

  @Test
  @DisplayName("스위치를 다시 올리면 원래대로 돌아온다")
  void reopensApply() {
    RecruitmentSchedule schedule = openScheduleWithToggle(false);
    schedule.changeApplyEnabled(true);
    recruitmentScheduleRepository.save(schedule);

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.applyEnabled()).isTrue();
    assertThat(status.message()).isEqualTo("서류 마감까지 D-5");
  }

  @Test
  @DisplayName("서류 기간이 아니면 스위치가 올라가 있어도 지원할 수 없다")
  void scheduleStillWinsOverToggle() {
    Generation generation = activeGeneration();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), NOW.plusDays(1), NOW.plusDays(30),
        NOW.plusDays(3), NOW.plusDays(10), NOW.plusDays(20)));

    RecruitmentStatusResult status = recruitmentStatusService.getStatus();

    assertThat(status.phase()).isEqualTo(RecruitmentPhase.BEFORE_OPEN);
    assertThat(status.applyEnabled()).isFalse();
    assertThat(status.message()).isEqualTo("모집 시작까지 D-3");
  }
}
