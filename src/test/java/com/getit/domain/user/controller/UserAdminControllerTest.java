package com.getit.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.dto.PromoteRequest;
import com.getit.domain.user.dto.UserUpdateRequest;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 9.1~9.5 /api/admin/users */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserAdminControllerTest {

  private static final String USERS_PATH = "/api/admin/users";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private User guest(String providerId, String email, String name) {
    return userRepository.save(User.createGuest(providerId, email, name, "https://cdn.getit.com/1.png"));
  }

  private String tokenFor(User user, Role role) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), role);
  }

  private String adminToken() {
    User admin = guest("google-admin", "admin@getit.com", "운영진");
    admin.updateRole(Role.ADMIN);
    return tokenFor(admin, Role.ADMIN);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(USERS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      User member = guest("google-member", "member@getit.com", "부원");

      mockMvc.perform(get(USERS_PATH).header("Authorization", tokenFor(member, Role.MEMBER)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + USERS_PATH)
  class GetUsers {

    @Test
    @DisplayName("keyword 로 사용자를 검색한다")
    void searchesByKeyword() throws Exception {
      guest("google-1", "a@getit.com", "김부원");
      guest("google-2", "b@getit.com", "이회원");

      mockMvc.perform(get(USERS_PATH)
              .param("keyword", "김")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content.length()").value(1))
          .andExpect(jsonPath("$.data.content[0].name").value("김부원"));
    }

    @Test
    @DisplayName("groupId=none 이면 미배정자만 반환한다")
    void filtersUnassignedOnly() throws Exception {
      Group group = groupRepository.save(Group.create(1L, "1조"));
      User grouped = guest("google-3", "c@getit.com", "조원");
      grouped.assignToGroup(group.getId());
      guest("google-4", "d@getit.com", "미배정");

      mockMvc.perform(get(USERS_PATH)
              .param("groupId", "none")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[?(@.name=='미배정')]").exists())
          .andExpect(jsonPath("$.data.content[?(@.name=='조원')]").doesNotExist());
    }
  }

  @Nested
  @DisplayName("PUT " + USERS_PATH + "/{id}")
  class UpdateUser {

    @Test
    @DisplayName("role 을 변경하면 활성 기수가 함께 붙는다")
    void updatesRole() throws Exception {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generationRepository.save(generation);
      User target = guest("google-5", "e@getit.com", "부원");
      String body = objectMapper.writeValueAsString(new UserUpdateRequest(Role.MEMBER, null, null));

      // 기수 없이 부원이 되면 강좌 · 대시보드가 403 이 된다 (이슈 #178).
      mockMvc.perform(put(USERS_PATH + "/" + target.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.role").value("MEMBER"))
          .andExpect(jsonPath("$.data.generationNo").value(9));
    }

    @Test
    @DisplayName("본인의 ADMIN 권한을 스스로 해제하려 하면 403 이다")
    void rejectsSelfAdminRevocation() throws Exception {
      User admin = guest("google-6", "f@getit.com", "운영진");
      admin.updateRole(Role.ADMIN);
      String selfToken = tokenFor(admin, Role.ADMIN);
      String body = objectMapper.writeValueAsString(new UserUpdateRequest(Role.MEMBER, null, null));

      mockMvc.perform(put(USERS_PATH + "/" + admin.getId())
              .header("Authorization", selfToken)
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("CANNOT_REMOVE_OWN_ADMIN"));
    }

    @Test
    @DisplayName("없는 사용자면 404 다")
    void returns404WhenNotFound() throws Exception {
      String body = objectMapper.writeValueAsString(new UserUpdateRequest(Role.MEMBER, null, null));

      mockMvc.perform(put(USERS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + USERS_PATH + "/{id}")
  class DeleteUser {

    @Test
    @DisplayName("사용자를 삭제(soft delete)한다")
    void deletesUser() throws Exception {
      User target = guest("google-7", "g@getit.com", "부원");

      mockMvc.perform(delete(USERS_PATH + "/" + target.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(userRepository.findById(target.getId()).orElseThrow().isDeleted()).isTrue();
    }

    @Test
    @DisplayName("본인을 삭제하려 하면 403 이고 탈퇴 처리되지 않는다")
    void rejectsSelfDeletion() throws Exception {
      User admin = guest("google-8", "h@getit.com", "운영진");
      admin.updateRole(Role.ADMIN);

      mockMvc.perform(delete(USERS_PATH + "/" + admin.getId())
              .header("Authorization", tokenFor(admin, Role.ADMIN)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("CANNOT_REMOVE_OWN_ADMIN"));

      assertThat(userRepository.findById(admin.getId()).orElseThrow().isDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("POST " + USERS_PATH + "/promote")
  class Promote {

    @Test
    @DisplayName("기수의 FINAL_PASS 지원자를 일괄 승격한다")
    void promotesFinalPassApplicants() throws Exception {
      Generation generation = Generation.create(9, 2026);
      generation.activate();
      generation = generationRepository.save(generation);
      User applicant = guest("google-9", "i@getit.com", "지원자");
      Application application = applicationRepository.save(Application.createDraft(
          applicant.getId(), generation.getId(), "지원자", "app@getit.com", "010-1234-5678",
          null, null, 3, "2021110000"));
      application.submit(LocalDateTime.now());
      application.decideDocumentResult(true);
      application.decideFinalResult(true);
      String body = objectMapper.writeValueAsString(new PromoteRequest(generation.getId(), null));

      mockMvc.perform(post(USERS_PATH + "/promote")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.promotedCount").value(1))
          .andExpect(jsonPath("$.data.skippedCount").value(0));

      assertThat(userRepository.findById(applicant.getId()).orElseThrow().getRole()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("활성 기수가 없으면 404 다")
    void returns404WhenNoActiveGeneration() throws Exception {
      String body = objectMapper.writeValueAsString(new PromoteRequest(999L, null));

      mockMvc.perform(post(USERS_PATH + "/promote")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("ACTIVE_GENERATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("요청한 기수가 활성 기수와 다르면 404 다")
    void returns404WhenGenerationNotActive() throws Exception {
      Generation activeGeneration = Generation.create(9, 2026);
      activeGeneration.activate();
      generationRepository.save(activeGeneration);
      Generation inactiveGeneration = generationRepository.save(Generation.create(8, 2025));
      String body = objectMapper.writeValueAsString(new PromoteRequest(inactiveGeneration.getId(), null));

      mockMvc.perform(post(USERS_PATH + "/promote")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("GENERATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET " + USERS_PATH + "/export")
  class ExportExcel {

    @Test
    @DisplayName("사용자 목록을 엑셀 파일로 반환한다")
    void downloadsExcel() throws Exception {
      guest("google-9", "i@getit.com", "김부원");

      byte[] excel = mockMvc.perform(get(USERS_PATH + "/export")
              .param("keyword", "김")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(header().string(
              "Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .andReturn().getResponse().getContentAsByteArray();

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getLastRowNum()).isEqualTo(1);
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("김부원");
      }
    }
  }
}
