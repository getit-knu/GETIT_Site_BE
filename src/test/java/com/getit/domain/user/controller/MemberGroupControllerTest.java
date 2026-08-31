package com.getit.domain.user.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** GET /api/member/group (이슈 #148) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberGroupControllerTest {

  private static final String PATH = "/api/member/group";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private User guest(String providerId) {
    return userRepository.saveAndFlush(User.createGuest(
        providerId, providerId + "@getit.com", "사람", "https://cdn.getit.com/1.png"));
  }

  private User member(String providerId) {
    User user = guest(providerId);
    user.promoteToMember(activeGeneration.getGenerationNo());
    userRepository.flush();
    return user;
  }

  private String bearerFor(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  @Test
  @DisplayName("배정된 조의 이름과 조원을 준다")
  void returnsMyGroup() throws Exception {
    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
    User me = member("google-sub-group-ok");
    me.assignToGroup(group.getId());
    userRepository.flush();

    mockMvc.perform(get(PATH).header("Authorization", bearerFor(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.name").value("1조"))
        .andExpect(jsonPath("$.data.memberCount").value(1))
        .andExpect(jsonPath("$.data.members[0].userId").value(me.getId()));
  }

  @Test
  @DisplayName("아직 배정되지 않았으면 data 가 null 이다")
  void returnsNullDataWhenUnassigned() throws Exception {
    User me = member("google-sub-group-none");

    // 배정 전은 오류가 아니라 정상 상태라 404 로 알리지 않는다.
    mockMvc.perform(get(PATH).header("Authorization", bearerFor(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  @Test
  @DisplayName("GUEST 는 부원 API 를 쓸 수 없다")
  void guestIsForbidden() throws Exception {
    User guest = guest("google-sub-group-guest");

    mockMvc.perform(get(PATH).header("Authorization", bearerFor(guest)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void requiresAuthentication() throws Exception {
    mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
  }
}
