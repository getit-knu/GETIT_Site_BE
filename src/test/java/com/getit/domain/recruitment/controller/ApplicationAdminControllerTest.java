package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.dto.DocumentDecisionRequest;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveRequest;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
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

  @Autowired
  private EvaluationCriterionRepository evaluationCriterionRepository;

  @Autowired
  private ObjectMapper objectMapper;

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

    @Test
    @DisplayName("generationId 로 필터링하면 비활성 기수도 조회할 수 있다")
    void filtersByGenerationId() throws Exception {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      Application other = applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));
      other.submit(LocalDateTime.now());

      mockMvc.perform(get(APPLICATIONS_PATH)
              .header("Authorization", adminToken())
              .param("generationId", String.valueOf(otherGeneration.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("지난기수"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("submittedAt 이 같아도 id 로 안정적으로 정렬된다")
    void stableSortWhenSubmittedAtTies() throws Exception {
      LocalDateTime sameInstant = LocalDateTime.now();
      Application first = submitted(1L, "가나다");
      Application second = submitted(2L, "마바사");
      ReflectionTestUtils.setField(first, "submittedAt", sameInstant);
      ReflectionTestUtils.setField(second, "submittedAt", sameInstant);

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", adminToken()).param("size", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(second.getId()));
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

    @Test
    @DisplayName("DRAFT 상태면 404 다")
    void returns404WhenDraft() throws Exception {
      Application application = draft(1L, "홍길동");

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + application.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + APPLICATIONS_PATH + "/{id}/scores")
  class SaveScores {

    private String scoresRequestJson(Long criterionId, int score) throws Exception {
      return objectMapper.writeValueAsString(
          new EvaluationScoreSaveRequest(List.of(new EvaluationScoreItem(criterionId, score))));
    }

    @Test
    @DisplayName("점수를 저장한다")
    void savesScores() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 45)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalScore").value(45))
          .andExpect(jsonPath("$.data.scores[0].score").value(45));
    }

    @Test
    @DisplayName("배점을 초과하면 400 이다")
    void returns400WhenScoreExceedsMax() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 61)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("SCORE_EXCEEDS_MAX"));
    }

    @Test
    @DisplayName("존재하지 않는 기준이면 404 다")
    void returns404WhenCriterionNotFound() throws Exception {
      Application application = submitted(1L, "홍길동");

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(999L, 10)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("CRITERION_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 합불이 결정된 지원서는 409 다")
    void returns409WhenAlreadyDecided() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));
      applicationRepository.updateStatusIfCurrentStatus(
          application.getId(), ApplicationStatus.DOC_PASS, ApplicationStatus.SUBMITTED);

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 10)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_SUBMITTED"));
    }
  }

  @Nested
  @DisplayName("PATCH " + APPLICATIONS_PATH + "/{id}/decision")
  class Decide {

    private String decisionRequestJson(boolean passed) throws Exception {
      return objectMapper.writeValueAsString(new DocumentDecisionRequest(passed));
    }

    @Test
    @DisplayName("합불을 결정한다")
    void decides() throws Exception {
      Application application = submitted(1L, "홍길동");

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("DOC_PASS"));
    }

    @Test
    @DisplayName("DRAFT 상태면 404 다 (존재하지 않는 것과 동일하게 취급)")
    void returns404WhenDraft() throws Exception {
      Application application = draft(1L, "홍길동");

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 결정된 지원서를 다시 결정하려 하면 409 다")
    void returns409WhenAlreadyDecided() throws Exception {
      Application application = submitted(1L, "홍길동");
      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk());

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(false)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_SUBMITTED"));
    }
  }
}
