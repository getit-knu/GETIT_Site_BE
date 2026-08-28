package com.getit.domain.setting.feature.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.feature.dto.FeatureToggleRequest;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
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

/** 10.23~10.24 /api/admin/setting/features */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeatureToggleControllerTest {

  private static final String FEATURES_PATH = "/api/admin/setting/features";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @BeforeEach
  void seedToggles() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, false));
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String enabledJson(boolean enabled) throws Exception {
    return objectMapper.writeValueAsString(new FeatureToggleRequest(enabled));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(FEATURES_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(FEATURES_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + FEATURES_PATH)
  class GetFeatures {

    @Test
    @DisplayName("FeatureKey 선언 순서로 반환한다")
    void returnsInEnumOrder() throws Exception {
      mockMvc.perform(get(FEATURES_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].key").value("STOCK_GAME"))
          .andExpect(jsonPath("$.data[1].key").value("MOCK_INVESTMENT"))
          .andExpect(jsonPath("$.data[0].label").value(FeatureKey.STOCK_GAME.getLabel()));
    }
  }

  @Nested
  @DisplayName("PUT " + FEATURES_PATH + "/{key}")
  class UpdateFeature {

    @Test
    @DisplayName("토글하면 enabled 가 바뀐다")
    void togglesFeature() throws Exception {
      mockMvc.perform(put(FEATURES_PATH + "/STOCK_GAME")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(enabledJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.key").value("STOCK_GAME"))
          .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("알 수 없는 key 면 400 이다")
    void returns400WhenUnknownKey() throws Exception {
      mockMvc.perform(put(FEATURES_PATH + "/NONSENSE")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(enabledJson(true)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("enabled 가 없으면 400 이다")
    void returns400WhenEnabledMissing() throws Exception {
      mockMvc.perform(put(FEATURES_PATH + "/STOCK_GAME")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }
}
