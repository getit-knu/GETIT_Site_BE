package com.getit.domain.project.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.project.admin.dto.ProjectRejectRequest;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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

  @Autowired
  private FileAssetRepository fileAssetRepository;

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
        "프로젝트", "팀", "2025-FALL", "설명", List.of("React"),
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

  private MockHttpServletRequestBuilder rejectRequest(Long projectId, String reason) throws Exception {
    return post(PATH + "/" + projectId + "/reject")
        .header("Authorization", adminToken())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(new ProjectRejectRequest(reason)));
  }

  /** 부원이 낸 승인 대기 프로젝트. (이슈 #148) */
  private Long pendingProjectId() {
    ProjectCommand command = new ProjectCommand(
        "부원 프로젝트", "3조", "2026-SPRING", null, List.of(), null, null, false, null);
    return projectRepository.save(Project.submit(command, 1, null)).getId();
  }

  @Test
  @DisplayName("승인하면 200 과 APPROVED 를 준다")
  void approvesPendingProject() throws Exception {
    Long projectId = pendingProjectId();

    mockMvc.perform(post(PATH + "/" + projectId + "/approve").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(ProjectStatus.APPROVED.name()))
        .andExpect(jsonPath("$.data.statusLabel").value("공개"));
  }

  @Test
  @DisplayName("반려하면 200 과 REJECTED · 사유를 준다")
  void rejectsPendingProject() throws Exception {
    Long projectId = pendingProjectId();

    mockMvc.perform(rejectRequest(projectId, "설명이 너무 짧습니다"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(ProjectStatus.REJECTED.name()))
        .andExpect(jsonPath("$.data.rejectReason").value("설명이 너무 짧습니다"));
  }

  @Test
  @DisplayName("사유 없이 반려할 수 없다")
  void requiresRejectReason() throws Exception {
    Long projectId = pendingProjectId();

    // 사유가 비면 부원은 지금과 똑같이 이유를 모른다. 그게 이 기능이 있는 이유다 (이슈 #190).
    mockMvc.perform(rejectRequest(projectId, "  "))
        .andExpect(status().isBadRequest());
    mockMvc.perform(post(PATH + "/" + projectId + "/reject")
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("다시 승인하면 반려 사유가 지워진다")
  void clearsReasonWhenApprovedAgain() throws Exception {
    Long projectId = pendingProjectId();
    mockMvc.perform(rejectRequest(projectId, "설명 보강 필요"));

    // 남겨 두면 공개된 프로젝트에 반려 사유가 붙어 있는 상태가 된다.
    mockMvc.perform(post(PATH + "/" + projectId + "/approve").header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value(ProjectStatus.APPROVED.name()))
        .andExpect(jsonPath("$.data.rejectReason").doesNotExist());
  }

  @Test
  @DisplayName("status 로 승인 대기만 모아 본다")
  void filtersByStatus() throws Exception {
    Long pending = pendingProjectId();
    Long rejected = pendingProjectId();
    mockMvc.perform(rejectRequest(rejected, "사유"));

    mockMvc.perform(get(PATH).param("status", ProjectStatus.PENDING.name())
            .header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(1))
        .andExpect(jsonPath("$.data.content[0].id").value(pending));
  }

  @Test
  @DisplayName("status 를 생략하면 상태와 무관하게 전부 나온다")
  void listsAllWithoutStatus() throws Exception {
    pendingProjectId();
    Long rejected = pendingProjectId();
    mockMvc.perform(rejectRequest(rejected, "사유"));

    // 어드민은 승인 대기 중인 것도 봐야 하므로 기본이 전체다 (이슈 #148).
    mockMvc.perform(get(PATH).header("Authorization", adminToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content.length()").value(2));
  }

  @Test
  @DisplayName("이미 같은 상태면 409 다")
  void rejectsNoOpTransition() throws Exception {
    Long projectId = pendingProjectId();
    mockMvc.perform(post(PATH + "/" + projectId + "/approve").header("Authorization", adminToken()));

    mockMvc.perform(post(PATH + "/" + projectId + "/approve").header("Authorization", adminToken()))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("승인 · 반려도 토큰이 없으면 401 이다")
  void decisionRejectsAnonymous() throws Exception {
    Long projectId = pendingProjectId();

    mockMvc.perform(post(PATH + "/" + projectId + "/approve")).andExpect(status().isUnauthorized());
    mockMvc.perform(post(PATH + "/" + projectId + "/reject")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("승인 · 반려도 ADMIN 이 아니면 403 이다")
  void decisionRejectsNonAdmin() throws Exception {
    Long projectId = pendingProjectId();
    String token = "Bearer " + jwtProvider.createAccessToken(adminId, "admin@getit.com", Role.MEMBER);

    mockMvc.perform(post(PATH + "/" + projectId + "/approve").header("Authorization", token))
        .andExpect(status().isForbidden());
    mockMvc.perform(post(PATH + "/" + projectId + "/reject")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ProjectRejectRequest("사유"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("선택 URL 을 빈 문자열로 보내도 등록된다")
  void acceptsBlankOptionalUrls() throws Exception {
    // 화면의 입력칸을 비워 두면 브라우저는 null 이 아니라 "" 를 보낸다.
    // @Pattern 은 null 은 검사하지 않지만 빈 문자열은 검사해서 400 이 났다 (이슈 #176).
    String body = objectMapper.writeValueAsString(new ProjectRequest.Write(
        "프로젝트", "팀", "2025-FALL", "설명", List.of(), "", "", null, true, null));

    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("http · https 가 아닌 주소는 여전히 막힌다")
  void stillRejectsNonHttpUrl() throws Exception {
    String body = objectMapper.writeValueAsString(new ProjectRequest.Write(
        "프로젝트", "팀", "2025-FALL", "설명", List.of(),
        "javascript:alert(1)", null, null, true, null));

    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());
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
        "프로젝트", "팀", "2025-FALL", null, List.of("React, Redux"),
        null, null, null, true, null));

    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("semester 형식이 YYYY-SEASON 이 아니면 400 이다")
  void rejectsMalformedSemester() throws Exception {
    String json = objectMapper.writeValueAsString(new ProjectRequest.Write(
        "프로젝트", "팀", "2025 Fall", null, List.of("React"),
        null, null, null, true, null));

    mockMvc.perform(post(PATH)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
  }

  @Test
  @DisplayName("수정 응답에 fileId 가 포함된다 (이슈 #139)")
  void updateResponseIncludesFileId() throws Exception {
    ProjectCommand command = new ProjectCommand(
        "원본", "팀", "2025-FALL", null, List.of(), null, null, false, null);
    Long id = projectRepository.save(Project.create(command, 1)).getId();
    Long fileId = fileAssetRepository.save(FileAsset.upload(
        "thumb", "thumb.png", "https://cdn/thumb.png", 10L, "image/png", 1L)).getId();

    String json = objectMapper.writeValueAsString(new ProjectRequest.Write(
        "수정", "팀", "2025-FALL", "설명", List.of("React"),
        "https://code", "https://demo", fileId, true, null));

    mockMvc.perform(put(PATH + "/" + id)
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fileId").value(fileId.intValue()));
  }

  @Test
  @DisplayName("삭제하면 204 다")
  void deletes() throws Exception {
    ProjectCommand command = new ProjectCommand(
        "삭제대상", "팀", "2025-FALL", null, List.of(), null, null, false, null);
    Long id = projectRepository.save(Project.create(command, 1)).getId();

    mockMvc.perform(delete(PATH + "/" + id).header("Authorization", adminToken()))
        .andExpect(status().isNoContent());
  }
}
