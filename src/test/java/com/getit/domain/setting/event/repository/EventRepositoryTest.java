package com.getit.domain.setting.event.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.global.config.JpaAuditingConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class EventRepositoryTest {

  @Autowired
  private EventRepository eventRepository;

  private Event event(long generationId, LocalDate startDate, String title) {
    return Event.create(
        new EventCommand(title, "장소", startDate, startDate, true, EventType.EVENT), generationId);
  }

  @Test
  @DisplayName("기수의 행사를 startDate 오름차순으로 조회한다")
  void findsByGenerationIdOrderByStartDateAscIdAsc() {
    eventRepository.save(event(9L, LocalDate.of(2026, 5, 1), "5월 행사"));
    eventRepository.save(event(9L, LocalDate.of(2026, 3, 1), "3월 행사"));
    eventRepository.save(event(8L, LocalDate.of(2026, 1, 1), "지난 기수 행사"));

    assertThat(eventRepository.findByGenerationIdOrderByStartDateAscIdAsc(9L))
        .extracting(Event::getTitle)
        .containsExactly("3월 행사", "5월 행사");
  }

  @Test
  @DisplayName("startDate 가 같으면 id 오름차순으로 정렬한다")
  void tieBreaksById() {
    Event first = eventRepository.save(event(9L, LocalDate.of(2026, 3, 1), "A"));
    Event second = eventRepository.save(event(9L, LocalDate.of(2026, 3, 1), "B"));

    assertThat(eventRepository.findByGenerationIdOrderByStartDateAscIdAsc(9L))
        .extracting(Event::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("id 와 소속 기수가 둘 다 일치할 때만 조회한다")
  void findsByIdAndGenerationIdOnlyWhenBothMatch() {
    Event saved = eventRepository.save(event(9L, LocalDate.of(2026, 3, 1), "행사"));

    assertThat(eventRepository.findByIdAndGenerationId(saved.getId(), 9L)).isPresent();
    assertThat(eventRepository.findByIdAndGenerationId(saved.getId(), 8L)).isEmpty();
  }
}
