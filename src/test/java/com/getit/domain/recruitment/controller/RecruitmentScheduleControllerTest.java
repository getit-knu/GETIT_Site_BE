package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.dto.ApplyToggleRequest;
import com.getit.domain.recruitment.dto.RecruitmentScheduleUpdateRequest;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

/** 6.1 GET · 6.2 PUT /api/admin/recruitment/schedule */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecruitmentScheduleControllerTest {

  private static final String SCHEDULE_PATH = "/api/admin/recruitment/schedule";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

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

  private LocalDateTime dt(int month, int day) {
    return LocalDateTime.of(2026, month, day, 0, 0);
  }

  /** Jackson 은 초가 0 이어도 생략하지 않는다. LocalDateTime#toString() 과 달라 별도로 맞춘다. */
  private String iso(LocalDateTime dateTime) {
    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
  }

  private String updateRequestJson(int totalStartMonth) throws Exception {
    RecruitmentScheduleUpdateRequest request = new RecruitmentScheduleUpdateRequest(
        dt(totalStartMonth, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15));
    return objectMapper.writeValueAsString(request);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(SCHEDULE_PATH))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(SCHEDULE_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + SCHEDULE_PATH)
  class GetSchedule {

    @Test
    @DisplayName("활성 기수의 일정을 반환한다")
    void returnsSchedule() throws Exception {
      recruitmentScheduleRepository.save(RecruitmentSchedule.create(
          activeGeneration.getId(),
          dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));

      mockMvc.perform(get(SCHEDULE_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.generationId").value(activeGeneration.getId()))
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.year").value(2026))
          .andExpect(jsonPath("$.data.interviewEndAt").value(iso(dt(9, 30))));
    }

    @Test
    @DisplayName("활성 기수가 없으면 404 다")
    void returns404WhenNoActiveGeneration() throws Exception {
      activeGeneration.deactivate();
      generationRepository.flush();

      mockMvc.perform(get(SCHEDULE_PATH).header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("ACTIVE_GENERATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("일정이 등록되지 않았으면 404 다")
    void returns404WhenScheduleNotFound() throws Exception {
      mockMvc.perform(get(SCHEDULE_PATH).header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + SCHEDULE_PATH)
  class UpdateSchedule {

    @Test
    @DisplayName("일정이 없으면 새로 생성하고 interviewEndAt 을 totalEndAt 으로 채운다")
    void createsSchedule() throws Exception {
      mockMvc.perform(put(SCHEDULE_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(updateRequestJson(9)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalStartAt").value(iso(dt(9, 1))))
          .andExpect(jsonPath("$.data.interviewEndAt").value(iso(dt(9, 30))));
    }

    @Test
    @DisplayName("검증에 실패하면 400 이다")
    void returns400OnInvalidOrder() throws Exception {
      // totalStartAt(10월) 이 totalEndAt(9월) 보다 늦다
      mockMvc.perform(put(SCHEDULE_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(updateRequestJson(10)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("활성 기수가 없으면 404 다")
    void returns404WhenNoActiveGeneration() throws Exception {
      activeGeneration.deactivate();
      generationRepository.flush();

      mockMvc.perform(put(SCHEDULE_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(updateRequestJson(9)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("ACTIVE_GENERATION_NOT_FOUND"));
    }
  }

  /**
   * 지원 접수 스위치. (이슈 #170)
   *
   * <p>일정과 별개다. 서류 기간 중이라도 내리면 지원이 막히고, 일정 값은 그대로 남는다.
   */
  @Nested
  @DisplayName("PUT " + SCHEDULE_PATH + "/apply-enabled")
  class ChangeApplyEnabled {

    private String togglePath() {
      return SCHEDULE_PATH + "/apply-enabled";
    }

    private String toggleJson(Boolean enabled) throws Exception {
      return objectMapper.writeValueAsString(new ApplyToggleRequest(enabled));
    }

    private void saveSchedule() {
      recruitmentScheduleRepository.save(RecruitmentSchedule.create(
          activeGeneration.getId(), dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));
    }

    @Test
    @DisplayName("내리면 applyEnabled 가 false 가 되고 일정은 그대로다")
    void disablesApply() throws Exception {
      saveSchedule();

      mockMvc.perform(put(togglePath())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(false)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.applyEnabled").value(false))
          .andExpect(jsonPath("$.data.documentEndAt").value(iso(dt(9, 10))));
    }

    @Test
    @DisplayName("다시 올리면 true 로 돌아온다")
    void enablesApplyAgain() throws Exception {
      saveSchedule();
      mockMvc.perform(put(togglePath())
          .header("Authorization", adminToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(toggleJson(false)));

      mockMvc.perform(put(togglePath())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.applyEnabled").value(true));
    }

    @Test
    @DisplayName("enabled 가 없으면 400 이다")
    void rejectsMissingEnabled() throws Exception {
      saveSchedule();

      // 여닫는 것은 사고가 났을 때 누르는 버튼이라, 값이 빠졌을 때 한쪽으로 기울면 안 된다.
      mockMvc.perform(put(togglePath())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(null)))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일정이 없으면 404 다")
    void returns404WhenNoSchedule() throws Exception {
      mockMvc.perform(put(togglePath())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(false)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }

    @Test
    @DisplayName("토큰이 없으면 401, ADMIN 이 아니면 403 이다")
    void requiresAdmin() throws Exception {
      saveSchedule();
      String memberToken =
          "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(put(togglePath())
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(false)))
          .andExpect(status().isUnauthorized());
      mockMvc.perform(put(togglePath())
              .header("Authorization", memberToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(toggleJson(false)))
          .andExpect(status().isForbidden());
    }
  }
}
