package com.getit.domain.setting.home.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.home.dto.HomeSaveRequest;
import com.getit.domain.user.entity.Role;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 10.20 POST /api/admin/setting/home/save */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeAdminControllerTest {

  private static final String SAVE_PATH = "/api/admin/setting/home/save";

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

  private HomeSaveRequest validRequest(Generation generation) {
    LocalDateTime now = LocalDateTime.now();
    return new HomeSaveRequest(
        new HomeSaveRequest.GenerationInfo(generation.getGenerationNo(), generation.getYear()),
        new HomeSaveRequest.ScheduleInfo(
            now.minusDays(1), now.plusDays(10), now.minusDays(1), now.plusDays(5), now.plusDays(6)),
        List.of(),
        List.of(new HomeSaveRequest.CurriculumInfo(null, "Python & 데이터 분석", "부제")),
        List.of(new HomeSaveRequest.EventInfo(
            null, "GETIT Chat", "IT5호관", LocalDate.now().plusDays(1), LocalDate.now().plusDays(1),
            EventType.EVENT, true)),
        List.of(new HomeSaveRequest.FaqInfo(null, "동아리 활동 시간은?", "매주 화요일", true)));
  }

  @Test
  @DisplayName("ADMIN 이 홈 화면 전체 상태를 한 번에 저장한다")
  void savesHomeAsAdmin() throws Exception {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);

    mockMvc.perform(post(SAVE_PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest(generation))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.generationNo").value(9));
  }

  @Test
  @DisplayName("MEMBER 권한으로는 접근할 수 없다")
  void forbidsNonAdmin() throws Exception {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    String memberToken = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

    mockMvc.perform(post(SAVE_PATH)
            .header("Authorization", memberToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(validRequest(generation))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("필수 필드가 없으면 400 이다")
  void rejectsInvalidRequest() throws Exception {
    mockMvc.perform(post(SAVE_PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }
}
