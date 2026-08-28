package com.getit.domain.lecture.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private UserRepository userRepository;

  private Long memberId;
  private Long outsiderId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    memberId = member("member", 9).getId();
    outsiderId = member("outsider", 8).getId();
  }

  private User member(String providerId, int generationNo) {
    User user = User.createGuest(providerId, providerId + "@getit.com", providerId, null);
    user.promoteToMember(generationNo);
    return userRepository.save(user);
  }

  private String token(Long userId) {
    return "Bearer " + jwtProvider.createAccessToken(userId, userId + "@getit.com", Role.MEMBER);
  }

  @Test
  @DisplayName("토큰 없으면 401")
  void rejectsAnonymous() throws Exception {
    mockMvc.perform(get("/api/member/me/summary"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("활성 기수 부원이면 200")
  void returnsSummary() throws Exception {
    mockMvc.perform(get("/api/member/me/summary").header("Authorization", token(memberId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.stats.enrolledLectureCount").value(0));
  }

  @Test
  @DisplayName("다른 기수 부원이면 403")
  void forbidsOutsider() throws Exception {
    mockMvc.perform(get("/api/member/me/summary").header("Authorization", token(outsiderId)))
        .andExpect(status().isForbidden());
  }
}
