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
import com.getit.domain.recruitment.dto.ApplicationQuestionRequest;
import com.getit.domain.recruitment.dto.QuestionOrderRequest;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.util.List;
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

/** 6.3~6.7 /api/admin/recruitment/questions */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationQuestionControllerTest {

  private static final String QUESTIONS_PATH = "/api/admin/recruitment/questions";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

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

  private String textRequestJson(String content) throws Exception {
    return objectMapper.writeValueAsString(
        new ApplicationQuestionRequest(QuestionType.TEXT, content, true, null, null));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(QUESTIONS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(QUESTIONS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + QUESTIONS_PATH)
  class GetQuestions {

    @Test
    @DisplayName("기수의 질문을 order 순으로 반환한다")
    void returnsQuestionsInOrder() throws Exception {
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 2, QuestionType.TEXT, "두번째", false, 300, null));
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "첫번째", true, 300, null));

      mockMvc.perform(get(QUESTIONS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].content").value("첫번째"))
          .andExpect(jsonPath("$.data[1].content").value("두번째"));
    }
  }

  @Nested
  @DisplayName("POST " + QUESTIONS_PATH)
  class CreateQuestion {

    @Test
    @DisplayName("주관식 질문을 추가한다")
    void createsTextQuestion() throws Exception {
      mockMvc.perform(post(QUESTIONS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(textRequestJson("지원 동기를 작성해주세요")))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.order").value(1))
          .andExpect(jsonPath("$.data.maxLength").value(300));
    }

    @Test
    @DisplayName("체크박스 질문에 options 가 없으면 400 이다")
    void returns400WhenOptionsMissing() throws Exception {
      String body = objectMapper.writeValueAsString(
          new ApplicationQuestionRequest(QuestionType.CHECKBOX, "관심 트랙", true, null, null));

      mockMvc.perform(post(QUESTIONS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("PUT " + QUESTIONS_PATH + "/{id}")
  class UpdateQuestion {

    @Test
    @DisplayName("질문을 수정한다")
    void updatesQuestion() throws Exception {
      ApplicationQuestion question = applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "원래 내용", false, 300, null));

      mockMvc.perform(put(QUESTIONS_PATH + "/" + question.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(textRequestJson("수정된 내용")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("없는 질문이면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(QUESTIONS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(textRequestJson("내용")))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("QUESTION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + QUESTIONS_PATH + "/{id}")
  class DeleteQuestion {

    @Test
    @DisplayName("질문을 삭제한다")
    void deletesQuestion() throws Exception {
      ApplicationQuestion question = applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "내용", false, 300, null));

      mockMvc.perform(delete(QUESTIONS_PATH + "/" + question.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("PUT " + QUESTIONS_PATH + "/order")
  class ReorderQuestions {

    @Test
    @DisplayName("배열 순서대로 order 를 재부여한다")
    void reordersQuestions() throws Exception {
      ApplicationQuestion q1 = applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "1번", false, 300, null));
      ApplicationQuestion q2 = applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 2, QuestionType.TEXT, "2번", false, 300, null));

      String body = objectMapper.writeValueAsString(
          new QuestionOrderRequest(List.of(q2.getId(), q1.getId())));

      mockMvc.perform(put(QUESTIONS_PATH + "/order")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNoContent());

      assertThatOrderIs(q2.getId(), 1);
      assertThatOrderIs(q1.getId(), 2);
    }

    private void assertThatOrderIs(Long questionId, int expectedOrder) {
      ApplicationQuestion question = applicationQuestionRepository.findById(questionId).orElseThrow();
      assertThat(question.getOrder()).isEqualTo(expectedOrder);
    }
  }
}
