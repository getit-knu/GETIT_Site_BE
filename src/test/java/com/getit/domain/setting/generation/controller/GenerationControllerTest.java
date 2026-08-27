package com.getit.domain.setting.generation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.generation.dto.GenerationUpdateRequest;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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

/** 10.1 GET · 10.2 PUT /api/admin/setting/generation */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GenerationControllerTest {

  private static final String GENERATION_PATH = "/api/admin/setting/generation";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String updateRequestJson(Integer generationNo, Integer year) throws Exception {
    return objectMapper.writeValueAsString(new GenerationUpdateRequest(generationNo, year));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(GENERATION_PATH))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(GENERATION_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + GENERATION_PATH)
  class GetGeneration {

    @Test
    @DisplayName("활성 기수를 반환한다")
    void returnsActiveGeneration() throws Exception {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generationRepository.save(generation);

      mockMvc.perform(get(GENERATION_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.year").value(2026))
          .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    @DisplayName("활성 기수가 없으면 404 다")
    void returns404WhenNoActiveGeneration() throws Exception {
      mockMvc.perform(get(GENERATION_PATH).header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("ACTIVE_GENERATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + GENERATION_PATH)
  class UpdateGeneration {

    @Test
    @DisplayName("새 기수를 활성화하면 기존 활성 기수는 비활성화된다")
    void activatesNewGenerationAndDeactivatesPrevious() throws Exception {
      Generation previous = Generation.create(8, 2025);
      previous.activate();
      generationRepository.save(previous);

      mockMvc.perform(put(GENERATION_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(updateRequestJson(9, 2026)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.isActive").value(true));

      mockMvc.perform(get(GENERATION_PATH).header("Authorization", adminToken()))
          .andExpect(jsonPath("$.data.generationNo").value(9));
    }

    @Test
    @DisplayName("generationNo 가 없으면 400 이다")
    void returns400WhenGenerationNoMissing() throws Exception {
      mockMvc.perform(put(GENERATION_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(updateRequestJson(null, 2026)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }
}
