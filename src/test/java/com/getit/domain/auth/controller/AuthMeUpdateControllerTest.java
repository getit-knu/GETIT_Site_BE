package com.getit.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** PUT /api/auth/me — 내 프로필 수정 (이슈 #147) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthMeUpdateControllerTest {

  private static final String ME_PATH = "/api/auth/me";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private UserRepository userRepository;

  private User signUp(String providerId, String email) {
    return userRepository.saveAndFlush(
        User.createGuest(providerId, email, "구글이름", "https://cdn.getit.com/g.png"));
  }

  private String bearerFor(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  private String body(String name, String phoneNumber) {
    return """
        {"name": "%s", "phoneNumber": %s}
        """.formatted(name, phoneNumber == null ? "null" : "\"" + phoneNumber + "\"");
  }

  @Test
  @DisplayName("GUEST 도 자기 프로필을 고칠 수 있다")
  void guestCanEditOwnProfile() throws Exception {
    User guest = signUp("google-sub-guest-edit", "guest-edit@getit.com");
    assertThat(guest.getRole()).isEqualTo(Role.GUEST);

    mockMvc.perform(put(ME_PATH)
            .header("Authorization", bearerFor(guest))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("게스트", "010-1111-2222")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("게스트"))
        .andExpect(jsonPath("$.data.phoneNumber").value("010-1111-2222"))
        .andExpect(jsonPath("$.data.role").value(Role.GUEST.name()));
  }

  @Test
  @DisplayName("MEMBER 도 같은 엔드포인트를 쓴다")
  void memberUsesTheSameEndpoint() throws Exception {
    User member = signUp("google-sub-member-edit", "member-edit@getit.com");
    member.promoteToMember(9);
    userRepository.flush();

    mockMvc.perform(put(ME_PATH)
            .header("Authorization", bearerFor(member))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("김부원", null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("김부원"))
        .andExpect(jsonPath("$.data.role").value(Role.MEMBER.name()));
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void requiresAuthentication() throws Exception {
    mockMvc.perform(put(ME_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("이름", null)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("이름이 비면 400 이다")
  void rejectsBlankName() throws Exception {
    User guest = signUp("google-sub-blank-name", "blank@getit.com");

    mockMvc.perform(put(ME_PATH)
            .header("Authorization", bearerFor(guest))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("   ", null)))
        .andExpect(status().isBadRequest());
  }
}
