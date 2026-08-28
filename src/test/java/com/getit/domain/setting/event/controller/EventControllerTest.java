package com.getit.domain.setting.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.dto.EventRequest;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerTest {

  private static final String EVENTS_PATH = "/api/admin/setting/events";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private EventRepository eventRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String requestJson(Long generationId, String title, LocalDate startDate, LocalDate endDate)
      throws Exception {
    return objectMapper.writeValueAsString(new EventRequest(
        generationId, title, "장소", startDate, endDate, EventType.EVENT, true));
  }

  private Event savedEvent(LocalDate startDate, String title) {
    return eventRepository.save(Event.create(
        new EventCommand(title, "장소", startDate, startDate, true, EventType.EVENT),
        activeGeneration.getId()));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(EVENTS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(EVENTS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + EVENTS_PATH)
  class GetEvents {

    @Test
    @DisplayName("활성 기수의 행사를 startDate 순으로 반환한다")
    void returnsEventsInOrder() throws Exception {
      savedEvent(LocalDate.of(2026, 5, 1), "5월 행사");
      savedEvent(LocalDate.of(2026, 3, 1), "3월 행사");

      mockMvc.perform(get(EVENTS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].title").value("3월 행사"))
          .andExpect(jsonPath("$.data[1].title").value("5월 행사"));
    }
  }

  @Nested
  @DisplayName("POST " + EVENTS_PATH)
  class CreateEvent {

    @Test
    @DisplayName("행사를 추가한다")
    void createsEvent() throws Exception {
      mockMvc.perform(post(EVENTS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(
                  activeGeneration.getId(), "해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.title").value("해커톤"))
          .andExpect(jsonPath("$.data.isVisible").value(true));
    }

    @Test
    @DisplayName("generationId 가 활성 기수와 다르면 404 다")
    void returns404WhenGenerationMismatch() throws Exception {
      mockMvc.perform(post(EVENTS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(999L, "해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("GENERATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("startDate 가 endDate 보다 늦으면 400 이다")
    void returns400WhenPeriodInverted() throws Exception {
      mockMvc.perform(post(EVENTS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(
                  activeGeneration.getId(), "해커톤", LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 10))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_EVENT_PERIOD"));
    }

    @Test
    @DisplayName("title 이 100자를 넘으면 400 이다")
    void returns400WhenTitleTooLong() throws Exception {
      String tooLong = "가".repeat(101);

      mockMvc.perform(post(EVENTS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(
                  activeGeneration.getId(), tooLong, LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("isVisible 이 없으면 400 이다")
    void returns400WhenVisibleMissing() throws Exception {
      String json = """
          {"generationId":%d,"title":"해커톤","place":"장소",
           "startDate":"2026-05-10","endDate":"2026-05-11","type":"EVENT"}
          """.formatted(activeGeneration.getId());

      mockMvc.perform(post(EVENTS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(json))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("PUT " + EVENTS_PATH + "/{id}")
  class UpdateEvent {

    @Test
    @DisplayName("행사를 수정한다")
    void updatesEvent() throws Exception {
      Event event = savedEvent(LocalDate.of(2026, 3, 1), "OT");

      mockMvc.perform(put(EVENTS_PATH + "/" + event.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(
                  activeGeneration.getId(), "해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.title").value("해커톤"));
    }

    @Test
    @DisplayName("없는 행사면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(EVENTS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(
                  activeGeneration.getId(), "해커톤", LocalDate.of(2026, 5, 10), LocalDate.of(2026, 5, 11))))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("EVENT_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + EVENTS_PATH + "/{id}")
  class DeleteEvent {

    @Test
    @DisplayName("행사를 삭제한다")
    void deletesEvent() throws Exception {
      Event event = savedEvent(LocalDate.of(2026, 3, 1), "OT");

      mockMvc.perform(delete(EVENTS_PATH + "/" + event.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(eventRepository.findById(event.getId())).isEmpty();
    }
  }
}
