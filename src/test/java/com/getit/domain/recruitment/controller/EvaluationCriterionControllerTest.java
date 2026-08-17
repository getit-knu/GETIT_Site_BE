package com.getit.domain.recruitment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.dto.EvaluationCriterionRequest;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
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

/** 6.8~6.11 /api/admin/recruitment/criteria */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EvaluationCriterionControllerTest {

  private static final String CRITERIA_PATH = "/api/admin/recruitment/criteria";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private EvaluationCriterionRepository evaluationCriterionRepository;

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

  private String requestJson(String name, String guideline, int maxScore) throws Exception {
    return objectMapper.writeValueAsString(new EvaluationCriterionRequest(name, guideline, maxScore));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(CRITERIA_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(CRITERIA_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + CRITERIA_PATH)
  class GetCriteria {

    @Test
    @DisplayName("기준 목록과 총점 · 유효 여부를 반환한다")
    void returnsCriteriaWithTotalScore() throws Exception {
      evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));
      evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 2, "지원 동기", "가이드 라인", 40));

      mockMvc.perform(get(CRITERIA_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.criteria[0].name").value("전공 적합성"))
          .andExpect(jsonPath("$.data.criteria[1].name").value("지원 동기"))
          .andExpect(jsonPath("$.data.totalScore").value(100))
          .andExpect(jsonPath("$.data.valid").value(true));
    }
  }

  @Nested
  @DisplayName("POST " + CRITERIA_PATH)
  class CreateCriterion {

    @Test
    @DisplayName("평가 기준을 추가한다")
    void createsCriterion() throws Exception {
      mockMvc.perform(post(CRITERIA_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("전공 적합성", "전공 적합성 가이드 라인", 20)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.order").value(1))
          .andExpect(jsonPath("$.data.maxScore").value(20));
    }

    @Test
    @DisplayName("배점 합계가 100 을 초과하면 400 이다")
    void returns400WhenTotalExceeds100() throws Exception {
      evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 70));

      mockMvc.perform(post(CRITERIA_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("지원 동기", "가이드 라인", 40)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_CRITERIA_TOTAL"));
    }
  }

  @Nested
  @DisplayName("PUT " + CRITERIA_PATH + "/{id}")
  class UpdateCriterion {

    @Test
    @DisplayName("평가 기준을 수정한다")
    void updatesCriterion() throws Exception {
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "원래 이름", "원래 가이드 라인", 20));

      mockMvc.perform(put(CRITERIA_PATH + "/" + criterion.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("수정된 이름", "수정된 가이드 라인", 25)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("수정된 이름"))
          .andExpect(jsonPath("$.data.maxScore").value(25));
    }

    @Test
    @DisplayName("없는 기준이면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(CRITERIA_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("이름", "가이드 라인", 20)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("CRITERION_NOT_FOUND"));
    }

    @Test
    @DisplayName("활성 기수가 아닌 기준이면 404 다")
    void returns404WhenCriterionBelongsToInactiveGeneration() throws Exception {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      EvaluationCriterion otherCriterion = evaluationCriterionRepository.save(EvaluationCriterion.create(
          otherGeneration.getId(), 1, "지난 기수 기준", "가이드 라인", 20));

      mockMvc.perform(put(CRITERIA_PATH + "/" + otherCriterion.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("이름", "가이드 라인", 20)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("CRITERION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + CRITERIA_PATH + "/{id}")
  class DeleteCriterion {

    @Test
    @DisplayName("평가 기준을 삭제한다")
    void deletesCriterion() throws Exception {
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "이름", "가이드 라인", 20));

      mockMvc.perform(delete(CRITERIA_PATH + "/" + criterion.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(evaluationCriterionRepository.findById(criterion.getId())).isEmpty();
    }
  }
}
