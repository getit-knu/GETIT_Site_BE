package com.getit.domain.setting.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.event.dto.EventRequest;
import com.getit.domain.setting.event.dto.EventResult;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EventAdminServiceTest {

  @Autowired
  private EventAdminService eventAdminService;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private EventRequest request(String title, LocalDate startDate, LocalDate endDate) {
    return new EventRequest(
        activeGeneration.getId(), title, "장소", startDate, endDate, EventType.EVENT, true);
  }

  private Event savedEvent(long generationId, LocalDate startDate, String title) {
    return eventRepository.save(
        Event.create(title, "장소", startDate, startDate, true, EventType.EVENT, generationId));
  }

  @Nested
  @DisplayName("getEvents")
  class GetEvents {

    @Test
    @DisplayName("활성 기수의 행사를 startDate 순으로 반환한다")
    void returnsEventsInOrder() {
      savedEvent(activeGeneration.getId(), LocalDate.of(2026, 5, 1), "5월 행사");
      savedEvent(activeGeneration.getId(), LocalDate.of(2026, 3, 1), "3월 행사");
      savedEvent(99L, LocalDate.of(2026, 1, 1), "다른 기수 행사");

      List<EventResult> results = eventAdminService.getEvents();

      assertThat(results).extracting(EventResult::title).containsExactly("3월 행사", "5월 행사");
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> eventAdminService.getEvents())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createEvent")
  class CreateEvent {

    @Test
    @DisplayName("행사를 추가한다")
    void createsEvent() {
      EventResult saved = eventAdminService.createEvent(
          request("해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11)));

      assertThat(saved.title()).isEqualTo("해커톤");
      assertThat(saved.startDate()).isEqualTo(LocalDate.of(2026, 5, 10));
      assertThat(saved.isVisible()).isTrue();
    }

    @Test
    @DisplayName("요청 generationId 가 활성 기수와 다르면 예외가 발생한다")
    void throwsWhenGenerationMismatch() {
      EventRequest mismatched = new EventRequest(
          999L, "해커톤", "장소", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11),
          EventType.COMPETITION, true);

      assertThatThrownBy(() -> eventAdminService.createEvent(mismatched))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("startDate 가 endDate 보다 늦으면 예외가 발생한다")
    void throwsWhenPeriodInverted() {
      assertThatThrownBy(() -> eventAdminService.createEvent(
          request("해커톤", LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.INVALID_EVENT_PERIOD);
    }
  }

  @Nested
  @DisplayName("updateEvent")
  class UpdateEvent {

    @Test
    @DisplayName("행사를 수정한다")
    void updatesEvent() {
      Event event = savedEvent(activeGeneration.getId(), LocalDate.of(2026, 3, 1), "OT");

      EventResult updated = eventAdminService.updateEvent(
          event.getId(), request("해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11)));

      assertThat(updated.title()).isEqualTo("해커톤");
      assertThat(updated.endDate()).isEqualTo(LocalDate.of(2026, 5, 11));
    }

    @Test
    @DisplayName("다른 기수의 행사면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Event other = savedEvent(otherGeneration.getId(), LocalDate.of(2025, 3, 1), "지난 기수 행사");

      assertThatThrownBy(() -> eventAdminService.updateEvent(
          other.getId(), request("해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 행사면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> eventAdminService.updateEvent(
          999L, request("해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.EVENT_NOT_FOUND);
    }

    @Test
    @DisplayName("startDate 가 endDate 보다 늦으면 예외가 발생한다")
    void throwsWhenPeriodInverted() {
      Event event = savedEvent(activeGeneration.getId(), LocalDate.of(2026, 3, 1), "OT");

      assertThatThrownBy(() -> eventAdminService.updateEvent(
          event.getId(), request("OT", LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 1))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.INVALID_EVENT_PERIOD);
    }
  }

  @Nested
  @DisplayName("deleteEvent")
  class DeleteEvent {

    @Test
    @DisplayName("행사를 삭제한다")
    void deletesEvent() {
      Event event = savedEvent(activeGeneration.getId(), LocalDate.of(2026, 3, 1), "OT");

      eventAdminService.deleteEvent(event.getId());

      assertThat(eventRepository.findById(event.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 기수의 행사면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Event other = savedEvent(otherGeneration.getId(), LocalDate.of(2025, 3, 1), "지난 기수 행사");

      assertThatThrownBy(() -> eventAdminService.deleteEvent(other.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(EventErrorCode.EVENT_NOT_FOUND);
    }
  }
}
