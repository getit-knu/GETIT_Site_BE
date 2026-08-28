package com.getit.domain.project.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectAdminControllerTest {

  private static final String PATH = "/api/admin/projects";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private UserRepository userRepository;

  private Long adminId;

  @BeforeEach
  void setUp() {
    User admin = User.createGuest("admin", "admin@getit.com", "관리자", null);
    admin.updateRole(Role.ADMIN);
    adminId = userRepository.save(admin).getId();
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(adminId, "admin@getit.com", Role.ADMIN);
  }

  private String body() throws Exception {
    return objectMapper.writeValueAsString(new ProjectRequest.Write(
        "프로젝트", "팀", "2025 Fall", "설명", List.of("React"),
        "https://code", "https://demo", null, true, null));
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void rejectsAnonymous() throws Exception {
    mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("ADMIN 이 아니면 403 이다")
  void rejectsNonAdmin() throws Exception {
    String token = "Bearer " + jwtProvider.createAccessToken(adminId, "admin@getit.com", Role.MEMBER);

    mockMvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("등록하고 목록에서 조회한다")
  void createsAndLists() throws Exception {
    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.title").value("프로젝트"))
        .andExpect(jsonPath("$.data.order").value(1));

    mockMvc.perform(get(PATH).header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].title").value("프로젝트"));
  }

  @Test
  @DisplayName("기술 스택 이름에 쉼표가 있으면 400 이다")
  void rejectsCommaInTechStack() throws Exception {
    String json = objectMapper.writeValueAsString(new ProjectRequest.Write(
        "프로젝트", "팀", "2025 Fall", null, List.of("React, Redux"),
        null, null, null, true, null));

    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("삭제하면 204 다")
  void deletes() throws Exception {
    ProjectCommand command = new ProjectCommand(
        "삭제대상", "팀", "2025 Fall", null, List.of(), null, null, false, null);
    Long id = projectRepository.save(Project.create(command, 1)).getId();

    mockMvc.perform(delete(PATH + "/" + id).header("Authorization", adminToken()))
        .andExpect(status().isNoContent());
  }
}
