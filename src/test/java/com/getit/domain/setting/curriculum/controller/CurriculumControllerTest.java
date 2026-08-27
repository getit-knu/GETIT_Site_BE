package com.getit.domain.setting.curriculum.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.curriculum.dto.CurriculumRequest;
import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
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

/** 10.10~10.13 /api/admin/setting/curriculums */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CurriculumControllerTest {

  private static final String CURRICULUMS_PATH = "/api/admin/setting/curriculums";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private CurriculumRepository curriculumRepository;

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

  private String requestJson(Long generationId, String title, String subtitle, Integer order) throws Exception {
    return objectMapper.writeValueAsString(new CurriculumRequest(generationId, title, subtitle, order));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(CURRICULUMS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(CURRICULUMS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + CURRICULUMS_PATH)
  class GetCurriculums {

    @Test
    @DisplayName("활성 기수의 커리큘럼을 order 순으로 반환한다")
    void returnsCurriculumsInOrder() throws Exception {
      curriculumRepository.save(Curriculum.create(activeGeneration.getId(), 2, "웹 개발", "React, Node.js"));
      curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      mockMvc.perform(get(CURRICULUMS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].title").value("Python & 데이터 분석"))
          .andExpect(jsonPath("$.data[1].title").value("웹 개발"));
    }
  }

  @Nested
  @DisplayName("POST " + CURRICULUMS_PATH)
  class CreateCurriculum {

    @Test
    @DisplayName("커리큘럼을 추가한다")
    void createsCurriculum() throws Exception {
      mockMvc.perform(post(CURRICULUMS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(activeGeneration.getId(), "팀 프로젝트", "실전 프로젝트 경험", 4)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.title").value("팀 프로젝트"))
          .andExpect(jsonPath("$.data.order").value(4));
    }

    @Test
    @DisplayName("generationId 가 활성 기수와 다르면 404 다")
    void returns404WhenGenerationMismatch() throws Exception {
      mockMvc.perform(post(CURRICULUMS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(999L, "팀 프로젝트", "실전 프로젝트 경험", 4)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("GENERATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + CURRICULUMS_PATH + "/{id}")
  class UpdateCurriculum {

    @Test
    @DisplayName("커리큘럼을 수정한다")
    void updatesCurriculum() throws Exception {
      Curriculum curriculum = curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      mockMvc.perform(put(CURRICULUMS_PATH + "/" + curriculum.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(activeGeneration.getId(), "웹 개발", "React, Node.js", 2)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.title").value("웹 개발"));
    }

    @Test
    @DisplayName("없는 커리큘럼이면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(CURRICULUMS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(activeGeneration.getId(), "웹 개발", "React, Node.js", 2)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("CURRICULUM_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + CURRICULUMS_PATH + "/{id}")
  class DeleteCurriculum {

    @Test
    @DisplayName("커리큘럼을 삭제한다")
    void deletesCurriculum() throws Exception {
      Curriculum curriculum = curriculumRepository.save(
          Curriculum.create(activeGeneration.getId(), 1, "Python & 데이터 분석", "Python 기초"));

      mockMvc.perform(delete(CURRICULUMS_PATH + "/" + curriculum.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(curriculumRepository.findById(curriculum.getId())).isEmpty();
    }
  }
}
