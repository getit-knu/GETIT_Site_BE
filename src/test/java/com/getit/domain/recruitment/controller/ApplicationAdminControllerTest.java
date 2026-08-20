package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
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

/** 7.1~7.2 /api/admin/recruitment/applications */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationAdminControllerTest {

  private static final String APPLICATIONS_PATH = "/api/admin/recruitment/applications";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private GenerationRepository generationRepository;

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

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private Application draft(Long userId, String name) {
    return applicationRepository.save(Application.createDraft(
        userId, activeGeneration.getId(), name, name + "@gmail.com", "010-1234-5678",
        null, null, 2, "2021110000"));
  }

  private Application submitted(Long userId, String name) {
    Application application = draft(userId, name);
    application.submit(LocalDateTime.now());
    return application;
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(APPLICATIONS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH)
  class GetApplicants {

    @Test
    @DisplayName("DRAFT 를 제외한 지원자 목록을 페이징 응답으로 반환한다")
    void returnsApplicantsExcludingDraft() throws Exception {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
          .andExpect(jsonPath("$.data.content[1]").doesNotExist())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("status 로 필터링한다")
    void filtersByStatus() throws Exception {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      mockMvc.perform(get(APPLICATIONS_PATH)
              .header("Authorization", adminToken())
              .param("status", "DRAFT"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("김철수"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH + "/{id}")
  class GetApplicantDetail {

    @Test
    @DisplayName("지원자 상세를 반환한다")
    void returnsDetail() throws Exception {
      Application application = submitted(1L, "홍길동");
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + application.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(application.getId()))
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
          .andExpect(jsonPath("$.data.basicInfo.name").value("홍길동"))
          .andExpect(jsonPath("$.data.answers[0].answerText").value("지원 동기입니다."));
    }

    @Test
    @DisplayName("없는 지원서면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(get(APPLICATIONS_PATH + "/999").header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }
  }
}
