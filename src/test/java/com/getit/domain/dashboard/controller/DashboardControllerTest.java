package com.getit.domain.dashboard.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 5.1~5.5 /api/admin/dashboard */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  @Test
  @DisplayName("summary 는 ADMIN 만 조회할 수 있고, 활성 기수가 없어도 0으로 채워 응답한다")
  void returnsSummaryForAdmin() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/summary").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalApplicants").value(0))
        .andExpect(jsonPath("$.data.memberCount").value(0));
  }

  @Test
  @DisplayName("MEMBER 권한으로는 접근할 수 없다")
  void forbidsNonAdmin() throws Exception {
    String memberToken = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

    mockMvc.perform(get("/api/admin/dashboard/summary").header("Authorization", memberToken))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("recent-questions 는 빈 배열도 200 이다")
  void returnsRecentQuestions() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/recent-questions").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @DisplayName("size 가 0 이하면 400 이다")
  void rejectsNonPositiveSize() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/recent-questions?size=0").header("Authorization", adminToken()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("submission-status 는 활성 기수가 없어도 200 이다")
  void returnsSubmissionStatus() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/submission-status").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalMemberCount").value(0))
        .andExpect(jsonPath("$.data.weeks").isArray());
  }

  @Test
  @DisplayName("upcoming-events 는 활성 기수가 없어도 200 이다")
  void returnsUpcomingEvents() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/upcoming-events").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  @DisplayName("ongoing-lectures 는 활성 기수가 없어도 200 이다")
  void returnsOngoingLectures() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/ongoing-lectures").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
  }
}
