package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 3.1 GET /api/applications/form · 3.2 GET /api/applications/me */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationControllerTest {

  private static final String FORM_PATH = "/api/applications/form";
  private static final String ME_PATH = "/api/applications/me";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private void saveSchedule() {
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 10, 23, 59, 59),
        LocalDateTime.of(2026, 9, 15, 0, 0)));
  }

  private String guestToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "guest@getit.com", Role.GUEST);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(FORM_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GUEST 도 접근할 수 있다")
    void allowsGuest() throws Exception {
      saveSchedule();

      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("GET " + FORM_PATH)
  class GetForm {

    @Test
    @DisplayName("기수 · 단계 · 마감일 · 질문 목록을 반환한다")
    void returnsForm() throws Exception {
      saveSchedule();
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, 300, null));

      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.deadline").value("2026-09-10T23:59:59"))
          .andExpect(jsonPath("$.data.questions[0].content").value("지원 동기"))
          .andExpect(jsonPath("$.data.questions[0].placeholder").doesNotExist());
    }

    @Test
    @DisplayName("모집 일정이 없으면 404 다")
    void returns404WhenNoSchedule() throws Exception {
      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET " + ME_PATH)
  class GetMyApplication {

    @Test
    @DisplayName("지원서가 없으면 data 가 null 이다")
    void returnsNullWhenNoApplication() throws Exception {
      mockMvc.perform(get(ME_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("내 지원서와 답변을 반환한다")
    void returnsMyApplication() throws Exception {
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2));
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      mockMvc.perform(get(ME_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(application.getId()))
          .andExpect(jsonPath("$.data.status").value("DRAFT"))
          .andExpect(jsonPath("$.data.basicInfo.name").value("홍길동"))
          .andExpect(jsonPath("$.data.answers[0].answerText").value("지원 동기입니다."));
    }
  }
}
