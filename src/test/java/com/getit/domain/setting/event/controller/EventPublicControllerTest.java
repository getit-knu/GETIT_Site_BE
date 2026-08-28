package com.getit.domain.setting.event.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.2 GET /api/public/events */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventPublicControllerTest {

  private static final String EVENTS_PATH = "/api/public/events";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private EventRepository eventRepository;

  private Long generationId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationId = generationRepository.save(generation).getId();
  }

  @Test
  @DisplayName("인증 없이 그 달에 걸치는 노출 행사를 반환한다")
  void returnsMonthlyEventsWithoutAuthentication() throws Exception {
    eventRepository.save(Event.create(new EventCommand(
        "5월 행사", "장소", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 12), true, EventType.EVENT), generationId));
    eventRepository.save(Event.create(new EventCommand(
        "숨김", "장소", LocalDate.of(2026, 5, 20), LocalDate.of(2026, 5, 21), false, EventType.EVENT), generationId));

    mockMvc.perform(get(EVENTS_PATH).param("year", "2026").param("month", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.year").value(2026))
        .andExpect(jsonPath("$.data.month").value(5))
        .andExpect(jsonPath("$.data.events.length()").value(1))
        .andExpect(jsonPath("$.data.events[0].title").value("5월 행사"))
        .andExpect(jsonPath("$.data.events[0].type").value("EVENT"));
  }

  @Test
  @DisplayName("활성 기수가 없으면 빈 events 로 200 이다")
  void returnsEmptyWhenNoActiveGeneration() throws Exception {
    generationRepository.deleteAll();

    mockMvc.perform(get(EVENTS_PATH).param("year", "2026").param("month", "5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.events").isEmpty());
  }

  @Test
  @DisplayName("month 가 1~12 밖이면 400 이다")
  void rejectsMonthOutOfRange() throws Exception {
    mockMvc.perform(get(EVENTS_PATH).param("year", "2026").param("month", "13"))
        .andExpect(status().isBadRequest());
  }
}
