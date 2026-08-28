package com.getit.domain.setting.event.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.event.dto.EventCommand;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventTest {

  @Test
  @DisplayName("생성한다")
  void creates() {
    Event event = Event.create(new EventCommand(
        "신입 부원 OT", "공대 7호관 강당",
        LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2),
        true, EventType.EVENT), 9L);

    assertThat(event.getTitle()).isEqualTo("신입 부원 OT");
    assertThat(event.getPlace()).isEqualTo("공대 7호관 강당");
    assertThat(event.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(event.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 2));
    assertThat(event.isVisible()).isTrue();
    assertThat(event.getType()).isEqualTo(EventType.EVENT);
    assertThat(event.getGenerationId()).isEqualTo(9L);
  }

  @Test
  @DisplayName("수정한다")
  void updates() {
    Event event = Event.create(new EventCommand(
        "신입 부원 OT", "공대 7호관 강당",
        LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2),
        true, EventType.EVENT), 9L);

    event.update(new EventCommand(
        "해커톤", "미정",
        LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11),
        false, EventType.COMPETITION));

    assertThat(event.getTitle()).isEqualTo("해커톤");
    assertThat(event.getPlace()).isEqualTo("미정");
    assertThat(event.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    assertThat(event.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 11));
    assertThat(event.isVisible()).isFalse();
    assertThat(event.getType()).isEqualTo(EventType.COMPETITION);
  }
}
