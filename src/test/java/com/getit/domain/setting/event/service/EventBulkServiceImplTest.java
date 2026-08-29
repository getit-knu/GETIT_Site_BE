package com.getit.domain.setting.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.exception.EventErrorCode;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventBulkServiceImplTest {

  private static final int GENERATION_NO = 9;

  @Autowired
  private EventBulkService eventBulkService;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Long generationId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(GENERATION_NO, 2026);
    generation.activate();
    generationId = generationRepository.save(generation).getId();
  }

  private Event saved(long generationId, String title, LocalDate start) {
    return eventRepository.save(Event.create(
        new EventCommand(title, "장소", start, start, true, EventType.EVENT), generationId));
  }

  @Test
  @DisplayName("그 기수 행사만 수정·생성·삭제한다")
  void replacesGenerationEvents() {
    Event keep = saved(generationId, "유지", LocalDate.of(2026, 5, 1));
    Event gone = saved(generationId, "삭제될것", LocalDate.of(2026, 5, 2));
    Event otherGen = saved(99L, "다른기수", LocalDate.of(2026, 5, 3));

    eventBulkService.replaceAll(GENERATION_NO, List.of(
        new EventUpsert(keep.getId(), "수정됨", "새장소",
            LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 2), EventType.WORKSHOP, false),
        new EventUpsert(null, "신규", "장소",
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3), EventType.COMPETITION, true)));

    List<Event> mine = eventRepository.findByGenerationIdOrderByStartDateAscIdAsc(generationId);
    assertThat(mine).extracting(Event::getTitle).containsExactlyInAnyOrder("수정됨", "신규");
    Event modified = mine.stream().filter(e -> e.getId().equals(keep.getId())).findFirst().orElseThrow();
    assertThat(modified.getPlace()).isEqualTo("새장소");
    assertThat(modified.getType()).isEqualTo(EventType.WORKSHOP);
    assertThat(modified.isVisible()).isFalse();
    assertThat(eventRepository.findById(gone.getId())).isEmpty();
    assertThat(eventRepository.findById(otherGen.getId())).isPresent();
  }

  @Test
  @DisplayName("startDate 가 endDate 보다 늦으면 INVALID_EVENT_PERIOD")
  void invalidPeriodThrows() {
    assertThatThrownBy(() -> eventBulkService.replaceAll(GENERATION_NO, List.of(
        new EventUpsert(null, "거꾸로", "장소",
            LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 1), EventType.EVENT, true))))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", EventErrorCode.INVALID_EVENT_PERIOD);
  }

  @Test
  @DisplayName("없는 기수면 ACTIVE_GENERATION_NOT_FOUND")
  void unknownGenerationThrows() {
    assertThatThrownBy(() -> eventBulkService.replaceAll(404, List.of()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", EventErrorCode.ACTIVE_GENERATION_NOT_FOUND);
  }
}
