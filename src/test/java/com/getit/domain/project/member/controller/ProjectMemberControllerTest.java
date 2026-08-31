package com.getit.domain.project.member.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.project.entity.ProjectStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** POST /api/member/projects (이슈 #148) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectMemberControllerTest {

  private static final String PATH = "/api/member/projects";
  private static final String BODY = """
      {
        "title": "우리 조 프로젝트",
        "semester": "2026-SPRING",
        "description": "설명",
        "techStacks": ["Java"],
        "codeUrl": "https://github.com/getit-knu/x"
      }
      """;

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

  private String bearerFor(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  @Test
  @DisplayName("조에 속한 부원이 등록하면 201 과 승인 대기 상태를 준다")
  void submitsAsPending() throws Exception {
    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "3조"));
    User member = guest("google-sub-project-member");
    member.promoteToMember(activeGeneration.getGenerationNo());
    member.assignToGroup(group.getId());
    userRepository.flush();

    mockMvc.perform(post(PATH)
            .header("Authorization", bearerFor(member))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.status").value(ProjectStatus.PENDING.name()))
        .andExpect(jsonPath("$.data.teamName").value("3조"));
  }

  @Test
  @DisplayName("학기 형식이 어긋나면 400 이다")
  void rejectsMalformedSemester() throws Exception {
    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "3조"));
    User member = guest("google-sub-project-bad-semester");
    member.promoteToMember(activeGeneration.getGenerationNo());
    member.assignToGroup(group.getId());
    userRepository.flush();

    mockMvc.perform(post(PATH)
            .header("Authorization", bearerFor(member))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY.replace("2026-SPRING", "2026년 봄")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void requiresAuthentication() throws Exception {
    mockMvc.perform(post(PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("description 이 상한을 넘으면 400 이다")
  void rejectsTooLongDescription() throws Exception {
    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "3조"));
    User member = guest("google-sub-project-long-desc");
    member.promoteToMember(activeGeneration.getGenerationNo());
    member.assignToGroup(group.getId());
    userRepository.flush();

    // 상한이 없으면 검증을 통과한 뒤 TEXT 컬럼에 넣다가 500 이 난다.
    mockMvc.perform(post(PATH)
            .header("Authorization", bearerFor(member))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY.replace("\"설명\"", "\"" + "가".repeat(20_001) + "\"")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GUEST 는 부원 API 를 쓸 수 없다")
  void guestIsForbidden() throws Exception {
    User guest = guest("google-sub-project-guest");

    mockMvc.perform(post(PATH)
            .header("Authorization", bearerFor(guest))
            .contentType(MediaType.APPLICATION_JSON)
            .content(BODY))
        .andExpect(status().isForbidden());
  }
}
