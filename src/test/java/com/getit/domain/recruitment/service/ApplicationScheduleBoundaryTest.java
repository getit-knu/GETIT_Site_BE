package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.ApplicationDraftRequest;
import com.getit.domain.recruitment.dto.BasicInfo;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모집 기간의 경계에서 어떤 오류가 나오는지 본다. (이슈 #175, PR #179 리뷰)
 *
 * <p>시작 전과 마감 후를 한 오류로 묶어 두어, 모집 시작 직전에 들어온 지원자가 아직 오지도
 * 않은 기한이 지났다는 문구를 봤다.
 *
 * <p>{@code Clock} 을 고정한다. 시스템 시계를 그대로 쓰면 "시작 1 초 전" 같은 경계를 만들 수
 * 없고, 하필 그 경계가 이 로직의 판단 기준이다.
 */
@SpringBootTest
@Transactional
class ApplicationScheduleBoundaryTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 15, 0, 0);

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
    }
  }

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private UserRepository userRepository;

  private Long generationId;
  private Long userId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationId = generationRepository.save(generation).getId();

    userId = userRepository.save(User.createGuest(
        "google-boundary", "boundary@getit.com", "지원자", null)).getId();
  }

  private void saveSchedule(LocalDateTime documentStartAt, LocalDateTime documentEndAt) {
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generationId,
        documentStartAt.minusDays(1), documentEndAt.plusDays(10),
        documentStartAt, documentEndAt, documentEndAt.plusDays(3)));
  }

  private ApplicationDraftRequest draft() {
    return new ApplicationDraftRequest(
        new BasicInfo("홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, null), null, true);
  }

  @Test
  @DisplayName("시작 1 초 전이면 기한 지남이 아니라 모집 기간 아님이다")
  void justBeforeStartIsNotOpen() {
    saveSchedule(NOW.plusSeconds(1), NOW.plusDays(5));

    assertThatThrownBy(() -> applicationService.saveDraft(userId, draft()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", RecruitmentErrorCode.APPLICATION_NOT_OPEN);
  }

  @Test
  @DisplayName("시작 시각이 되면 지원할 수 있다")
  void opensExactlyAtStart() {
    saveSchedule(NOW, NOW.plusDays(5));

    assertThatCode(() -> applicationService.saveDraft(userId, draft())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("마감 시각까지는 지원할 수 있다")
  void staysOpenUntilDeadline() {
    saveSchedule(NOW.minusDays(5), NOW);

    assertThatCode(() -> applicationService.saveDraft(userId, draft())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("마감 1 초 뒤면 기한이 지났다")
  void justAfterDeadlineIsPassed() {
    saveSchedule(NOW.minusDays(5), NOW.minusSeconds(1));

    assertThatThrownBy(() -> applicationService.saveDraft(userId, draft()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", RecruitmentErrorCode.APPLICATION_DEADLINE_PASSED);
  }
}
