package com.getit.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.dto.GroupCreateRequest;
import com.getit.domain.user.dto.GroupMemberAddRequest;
import com.getit.domain.user.dto.GroupRenameRequest;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import java.util.List;
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

/** 9.6~9.11 /api/admin/groups */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GroupControllerTest {

  private static final String GROUPS_PATH = "/api/admin/groups";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private UserRepository userRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private User member(String providerId, String email) {
    User user = userRepository.save(
        User.createGuest(providerId, email, "부원", "https://cdn.getit.com/1.png"));
    user.promoteToMember(activeGeneration.getGenerationNo());
    return user;
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(GROUPS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(GROUPS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + GROUPS_PATH)
  class GetGroups {

    @Test
    @DisplayName("조 목록과 조원, 미배정자를 반환한다")
    void returnsGroupsWithMembersAndUnassigned() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User grouped = member("google-1", "a@getit.com");
      grouped.assignToGroup(group.getId());
      member("google-2", "b@getit.com");

      mockMvc.perform(get(GROUPS_PATH)
              .param("generationId", String.valueOf(activeGeneration.getId()))
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.groups[0].name").value("1조"))
          .andExpect(jsonPath("$.data.groups[0].memberCount").value(1))
          .andExpect(jsonPath("$.data.unassigned.length()").value(1));
    }
  }

  @Nested
  @DisplayName("POST " + GROUPS_PATH)
  class CreateGroup {

    @Test
    @DisplayName("조를 생성한다")
    void createsGroup() throws Exception {
      String body = objectMapper.writeValueAsString(
          new GroupCreateRequest(activeGeneration.getId(), "1조"));

      mockMvc.perform(post(GROUPS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.name").value("1조"))
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.memberCount").value(0));
    }

    @Test
    @DisplayName("이름이 중복되면 409 다")
    void returns409WhenNameDuplicate() throws Exception {
      groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      String body = objectMapper.writeValueAsString(
          new GroupCreateRequest(activeGeneration.getId(), "1조"));

      mockMvc.perform(post(GROUPS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("DUPLICATE_GROUP_NAME"));
    }
  }

  @Nested
  @DisplayName("PUT " + GROUPS_PATH + "/{id}")
  class RenameGroup {

    @Test
    @DisplayName("조 이름을 수정한다")
    void renamesGroup() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      String body = objectMapper.writeValueAsString(new GroupRenameRequest("A조"));

      mockMvc.perform(put(GROUPS_PATH + "/" + group.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("A조"));
    }

    @Test
    @DisplayName("없는 조면 404 다")
    void returns404WhenNotFound() throws Exception {
      String body = objectMapper.writeValueAsString(new GroupRenameRequest("A조"));

      mockMvc.perform(put(GROUPS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + GROUPS_PATH + "/{id}")
  class DeleteGroup {

    @Test
    @DisplayName("조를 삭제하면 조원은 미배정 상태가 된다")
    void deletesGroupAndUnassignsMembers() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User user = member("google-3", "c@getit.com");
      user.assignToGroup(group.getId());

      mockMvc.perform(delete(GROUPS_PATH + "/" + group.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(groupRepository.findById(group.getId())).isEmpty();
      assertThat(userRepository.findById(user.getId()).orElseThrow().getGroupId()).isNull();
    }
  }

  @Nested
  @DisplayName("POST " + GROUPS_PATH + "/{groupId}/members")
  class AddMembers {

    @Test
    @DisplayName("조원을 추가한다")
    void addsMembers() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User user = member("google-4", "d@getit.com");
      String body = objectMapper.writeValueAsString(new GroupMemberAddRequest(List.of(user.getId())));

      mockMvc.perform(post(GROUPS_PATH + "/" + group.getId() + "/members")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.addedCount").value(1))
          .andExpect(jsonPath("$.data.memberCount").value(1));
    }

    @Test
    @DisplayName("이미 다른 조에 속한 사용자면 409 다")
    void returns409WhenAlreadyInGroup() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      Group otherGroup = groupRepository.save(Group.create(activeGeneration.getId(), "2조"));
      User user = member("google-5", "e@getit.com");
      user.assignToGroup(otherGroup.getId());
      String body = objectMapper.writeValueAsString(new GroupMemberAddRequest(List.of(user.getId())));

      mockMvc.perform(post(GROUPS_PATH + "/" + group.getId() + "/members")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("ALREADY_IN_GROUP"));
    }
  }

  @Nested
  @DisplayName("DELETE " + GROUPS_PATH + "/{groupId}/members/{userId}")
  class RemoveMember {

    @Test
    @DisplayName("조원을 뺀다")
    void removesMember() throws Exception {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User user = member("google-6", "f@getit.com");
      user.assignToGroup(group.getId());

      mockMvc.perform(delete(GROUPS_PATH + "/" + group.getId() + "/members/" + user.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(userRepository.findById(user.getId()).orElseThrow().getGroupId()).isNull();
    }
  }
}
