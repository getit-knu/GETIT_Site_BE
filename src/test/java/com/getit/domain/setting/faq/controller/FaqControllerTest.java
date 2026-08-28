package com.getit.domain.setting.faq.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.dto.FaqRequest;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 10.18~10.19 /api/admin/setting/faqs */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaqControllerTest {

  private static final String FAQS_PATH = "/api/admin/setting/faqs";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FaqRepository faqRepository;

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String requestJson(String question, Integer order) throws Exception {
    return objectMapper.writeValueAsString(new FaqRequest(question, "답변입니다.", true, order));
  }

  private Faq saved(String question, int order) {
    return faqRepository.save(Faq.create(new FaqCommand(question, "답변입니다.", true), order));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(FAQS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(FAQS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + FAQS_PATH)
  class GetFaqs {

    @Test
    @DisplayName("FAQ 를 order 순으로 반환한다")
    void returnsFaqsInOrder() throws Exception {
      saved("B", 2);
      saved("A", 1);

      mockMvc.perform(get(FAQS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].question").value("A"))
          .andExpect(jsonPath("$.data[1].question").value("B"));
    }
  }

  @Nested
  @DisplayName("POST " + FAQS_PATH)
  class CreateFaq {

    @Test
    @DisplayName("FAQ 를 추가한다 (첫 항목이라 order 는 1로 clamp 된다)")
    void createsFaq() throws Exception {
      mockMvc.perform(post(FAQS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("가입 조건은?", 4)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.question").value("가입 조건은?"))
          .andExpect(jsonPath("$.data.order").value(1));
    }

    @Test
    @DisplayName("order 를 생략해도 추가된다")
    void createsFaqWithoutOrder() throws Exception {
      mockMvc.perform(post(FAQS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("가입 조건은?", null)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.order").value(1));
    }

    @Test
    @DisplayName("question 이 255자를 넘으면 400 이다")
    void returns400WhenQuestionTooLong() throws Exception {
      String tooLong = "가".repeat(256);

      mockMvc.perform(post(FAQS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(tooLong, 1)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("order 가 0 이하면 400 이다")
    void returns400WhenOrderNotPositive() throws Exception {
      mockMvc.perform(post(FAQS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("가입 조건은?", 0)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("PUT " + FAQS_PATH + "/{id}")
  class UpdateFaq {

    @Test
    @DisplayName("FAQ 를 수정한다")
    void updatesFaq() throws Exception {
      Faq faq = saved("가입 조건은?", 1);

      mockMvc.perform(put(FAQS_PATH + "/" + faq.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("활동 기간은?", 1)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.question").value("활동 기간은?"));
    }

    @Test
    @DisplayName("없는 FAQ 면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(FAQS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson("활동 기간은?", 1)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("FAQ_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + FAQS_PATH + "/{id}")
  class DeleteFaq {

    @Test
    @DisplayName("FAQ 를 삭제한다")
    void deletesFaq() throws Exception {
      Faq faq = saved("가입 조건은?", 1);

      mockMvc.perform(delete(FAQS_PATH + "/" + faq.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(faqRepository.findById(faq.getId())).isEmpty();
    }
  }
}
