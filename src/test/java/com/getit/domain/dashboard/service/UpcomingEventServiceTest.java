package com.getit.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.dashboard.dto.UpcomingEventResult;
import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

/** {@code Clock} 을 고정해서 dDay 를 결정적으로 검증한다({@code RecruitmentStatusServiceTest}와 동일 패턴). */
@SpringBootTest
@Transactional
class UpcomingEventServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 1);

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(TODAY.atStartOfDay(SEOUL).toInstant(), SEOUL);
    }
  }

  @Autowired
  private UpcomingEventService upcomingEventService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private EventRepository eventRepository;

  @Test
  @DisplayName("활성 기수의 다가오는 행사를 dDay 와 함께 가까운 순으로 반환한다")
  void returnsUpcomingEventsWithDDay() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    Long generationId = generationRepository.save(generation).getId();
    eventRepository.save(Event.create(
        new EventCommand("GETIT 해커톤 대회", "IT5호관 312호", TODAY.plusDays(10), TODAY.plusDays(10),
            true, EventType.COMPETITION),
        generationId));
    eventRepository.save(Event.create(
        new EventCommand("GETIT Chat", "IT5호관 312호", TODAY.plusDays(7), TODAY.plusDays(7),
            true, EventType.EVENT),
        generationId));

    List<UpcomingEventResult> results = upcomingEventService.getUpcomingEvents();

    assertThat(results).extracting(UpcomingEventResult::title)
        .containsExactly("GETIT Chat", "GETIT 해커톤 대회");
    assertThat(results.get(0).dDay()).isEqualTo(7L);
    assertThat(results.get(1).dDay()).isEqualTo(10L);
  }

  @Test
  @DisplayName("활성 기수가 없으면 빈 리스트다")
  void returnsEmptyWhenNoActiveGeneration() {
    assertThat(upcomingEventService.getUpcomingEvents()).isEmpty();
  }
}
