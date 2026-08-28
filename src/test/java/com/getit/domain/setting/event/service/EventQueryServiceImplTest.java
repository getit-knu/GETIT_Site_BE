package com.getit.domain.setting.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.LocalDate;
import java.time.ZoneId;
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
class EventQueryServiceImplTest {

  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

  @Autowired
  private EventQueryService eventQueryService;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private Event save(long generationId, LocalDate start, LocalDate end, boolean visible, String title) {
    return eventRepository.save(Event.create(
        new EventCommand(title, "장소", start, end, visible, EventType.EVENT), generationId));
  }

  @Nested
  @DisplayName("findByMonth")
  class FindByMonth {

    @Test
    @DisplayName("그 달에 걸치는 노출 행사만 startDate 순으로 반환한다")
    void returnsOverlappingVisibleEvents() {
      long gen = activeGeneration.getId();
      save(gen, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), true, "5월 중순");
      save(gen, LocalDate.of(2026, 4, 28), LocalDate.of(2026, 5, 2), true, "4월말~5월초");
      save(gen, LocalDate.of(2026, 5, 30), LocalDate.of(2026, 6, 3), true, "5월말~6월초");
      save(gen, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), true, "4월");
      save(gen, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 5), true, "6월");
      save(gen, LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 16), false, "비노출");
      save(99L, LocalDate.of(2026, 5, 15), LocalDate.of(2026, 5, 16), true, "다른 기수");

      List<EventView> result = eventQueryService.findByMonth(2026, 5);

      assertThat(result).extracting(EventView::title)
          .containsExactly("4월말~5월초", "5월 중순", "5월말~6월초");
    }

    @Test
    @DisplayName("활성 기수가 없으면 빈 리스트다")
    void emptyWhenNoActiveGeneration() {
      save(activeGeneration.getId(), LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), true, "5월");
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThat(eventQueryService.findByMonth(2026, 5)).isEmpty();
    }
  }

  @Nested
  @DisplayName("findUpcoming")
  class FindUpcoming {

    @Test
    @DisplayName("오늘 이후 시작하는 노출 행사만 가까운 순으로 반환한다")
    void returnsFutureVisibleEvents() {
      long gen = activeGeneration.getId();
      save(gen, TODAY.plusDays(10), TODAY.plusDays(11), true, "먼 행사");
      save(gen, TODAY.plusDays(2), TODAY.plusDays(3), true, "가까운 행사");
      save(gen, TODAY.minusDays(2), TODAY.minusDays(1), true, "지난 행사");
      save(gen, TODAY.plusDays(5), TODAY.plusDays(6), false, "비노출");

      List<EventView> result = eventQueryService.findUpcoming();

      assertThat(result).extracting(EventView::title).containsExactly("가까운 행사", "먼 행사");
    }

    @Test
    @DisplayName("활성 기수가 없으면 빈 리스트다")
    void emptyWhenNoActiveGeneration() {
      save(activeGeneration.getId(), TODAY.plusDays(2), TODAY.plusDays(3), true, "행사");
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThat(eventQueryService.findUpcoming()).isEmpty();
    }
  }
}
