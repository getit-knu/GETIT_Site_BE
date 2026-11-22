package com.getit.domain.festa.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.festa.repository.FestaVoteRepository;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.GroupRepository;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** 모의투자 FESTA 1인 1팀 투표 (이슈 #232) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FestaVoteControllerTest {

  private static final String VOTE = "/api/festa/vote";
  private static final String ADMIN = "/api/admin/festa";
  private static final String PHONE = "010-1234-5678";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private GenerationRepository generationRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private FeatureToggleRepository featureToggleRepository;
  @Autowired private FestaVoteRepository voteRepository;
  @Autowired private FestaInvestorRepository investorRepository;

  private Generation generation;
  private Group myGroup;
  private Project projectA;
  private Project projectB;
  private Project myGroupProject;
  private Project notEntered;

  @BeforeEach
  void setUp() {
    Generation g = Generation.create(9, 2026);
    g.activate();
    generation = generationRepository.save(g);
    myGroup = groupRepository.save(Group.create(generation.getId(), "3조"));
    Group otherGroup = groupRepository.save(Group.create(generation.getId(), "4조"));

    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, true));
    projectA = festaProject("A", 1, otherGroup.getId(), true);
    projectB = festaProject("B", 2, otherGroup.getId(), true);
    myGroupProject = festaProject("우리 조", 3, myGroup.getId(), true);
    notEntered = festaProject("미출전", 4, otherGroup.getId(), false);
  }

  private Project festaProject(String title, int festaOrder, Long groupId, boolean entry) {
    Project project = Project.submit(new ProjectCommand(
        title, "팀", "2026-FALL", "설명", List.of(), null, null, false, null), festaOrder, groupId);
    project.changeStatus(ProjectStatus.APPROVED);
    project.updateFesta(entry, festaOrder);
    return projectRepository.saveAndFlush(project);
  }

  private User guest(String id) {
    return userRepository.saveAndFlush(
        User.createGuest(id, id + "@getit.com", "홍길동" + id, "https://cdn.getit.com/1.png"));
  }

  private User memberOfMyGroup(String id) {
    User user = guest(id);
    user.promoteToMember(generation.getGenerationNo());
    user.assignToGroup(myGroup.getId());
    return userRepository.saveAndFlush(user);
  }

  private String bearer(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  /** 명단 CSV 는 투표가 끝난 뒤에만 받을 수 있다. 일정이 없으면 토글을 끈다. */
  private void closeFesta() {
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();
  }

  private String adminBearer() {
    User admin = userRepository.findAll().stream()
        .filter(user -> user.getRole() == Role.ADMIN)
        .findFirst()
        .orElseGet(() -> {
          User created = guest("vote-admin");
          created.updateRole(Role.ADMIN);
          return userRepository.saveAndFlush(created);
        });
    return "Bearer " + jwtProvider.createAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private ResultActions vote(User user, Long projectId, String phone, Boolean consent) throws Exception {
    String body = "{\"projectId\":%s,\"phoneNumber\":%s,\"privacyConsent\":%s}".formatted(
        projectId, phone == null ? "null" : "\"" + phone + "\"", consent);
    return mockMvc.perform(put(VOTE)
        .header("Authorization", bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  @Test
  @DisplayName("로그인 없이는 401 이다")
  void requiresLogin() throws Exception {
    mockMvc.perform(get(VOTE)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("투표 전에는 projectId 와 번호가 null 이다")
  void emptyBeforeVoting() throws Exception {
    mockMvc.perform(get(VOTE).header("Authorization", bearer(guest("v1"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.projectId").value(nullValue()))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value(nullValue()))
        .andExpect(jsonPath("$.data.ownProjectIds.length()").value(0));
  }

  @Test
  @DisplayName("GUEST 도 투표하고, 번호는 마스킹되어 돌아온다")
  void guestVotes() throws Exception {
    User guest = guest("v2");

    vote(guest, projectA.getId(), PHONE, true)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.projectId").value(projectA.getId()))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"));

    mockMvc.perform(get(VOTE).header("Authorization", bearer(guest)))
        .andExpect(jsonPath("$.data.projectId").value(projectA.getId()));
  }

  @Test
  @DisplayName("다시 투표하면 표가 옮겨가고 한 사람 한 표다. 번호는 안 보내도 유지된다")
  void revoteMovesTheVote() throws Exception {
    User guest = guest("v3");
    vote(guest, projectA.getId(), PHONE, true).andExpect(status().isOk());

    vote(guest, projectB.getId(), null, null)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.projectId").value(projectB.getId()))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"));

    org.assertj.core.api.Assertions.assertThat(voteRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("우리 조 프로젝트는 막고, 조회에는 ownProjectIds 가 나온다")
  void rejectsOwnGroup() throws Exception {
    User member = memberOfMyGroup("v4");

    vote(member, myGroupProject.getId(), PHONE, true)
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("OWN_GROUP_PROJECT"));
    mockMvc.perform(get(VOTE).header("Authorization", bearer(member)))
        .andExpect(jsonPath("$.data.ownProjectIds[0]").value(myGroupProject.getId()));
  }

  @Test
  @DisplayName("출전하지 않은 프로젝트와 없는 프로젝트는 거부한다")
  void rejectsNotEntered() throws Exception {
    vote(guest("v5"), notEntered.getId(), PHONE, true)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_ENTERED"));
    vote(guest("v6"), 999_999L, PHONE, true)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_ENTERED"));
  }

  @Test
  @DisplayName("projectId 가 없으면 400 이다")
  void requiresProjectId() throws Exception {
    vote(guest("v7"), null, PHONE, true).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("토글이 꺼지면 투표는 막히고 조회는 된다")
  void closedWhenToggleOff() throws Exception {
    User guest = guest("v8");
    vote(guest, projectA.getId(), PHONE, true).andExpect(status().isOk());
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();

    vote(guest, projectB.getId(), null, null)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_CLOSED"));
    mockMvc.perform(get(VOTE).header("Authorization", bearer(guest)))
        .andExpect(jsonPath("$.data.projectId").value(projectA.getId()));
  }

  @Test
  @DisplayName("번호 규칙: 첫 투표엔 필수, 형식과 동의 검사")
  void phoneRules() throws Exception {
    vote(guest("v9"), projectA.getId(), null, null)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PHONE_NUMBER_REQUIRED"));
    vote(guest("v10"), projectA.getId(), "01012345678", true)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_PHONE_NUMBER"));
    vote(guest("v11"), projectA.getId(), PHONE, false)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PRIVACY_CONSENT_REQUIRED"));
  }

  @Test
  @DisplayName("득표 집계는 내림차순이고 표 없는 출전 팀도 0 으로 나온다")
  void voteResults() throws Exception {
    vote(guest("v12"), projectB.getId(), PHONE, true).andExpect(status().isOk());
    vote(guest("v13"), projectB.getId(), "010-0000-0001", true).andExpect(status().isOk());
    vote(guest("v14"), projectA.getId(), "010-0000-0002", true).andExpect(status().isOk());

    mockMvc.perform(get(ADMIN + "/vote-results").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.voterCount").value(3))
        .andExpect(jsonPath("$.data.projects.length()").value(3))
        .andExpect(jsonPath("$.data.projects[0].projectId").value(projectB.getId()))
        .andExpect(jsonPath("$.data.projects[0].voteCount").value(2))
        .andExpect(jsonPath("$.data.projects[1].voteCount").value(1))
        .andExpect(jsonPath("$.data.projects[2].voteCount").value(0));
  }

  @Test
  @DisplayName("득표 집계는 어드민만 본다")
  void voteResultsAdminOnly() throws Exception {
    mockMvc.perform(get(ADMIN + "/vote-results").header("Authorization", bearer(memberOfMyGroup("v15"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("명단 CSV 에 투표자가 들어가고, 파기하면 번호가 비워진다")
  void csvIncludesVotersAndDestroy() throws Exception {
    vote(guest("v16"), projectA.getId(), PHONE, true).andExpect(status().isOk());

    closeFesta();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(PHONE)));

    mockMvc.perform(delete(ADMIN + "/investors/phones").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.destroyedCount").value(1));
    closeFesta();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString(PHONE))));
  }

  @Test
  @DisplayName("리허설 초기화는 투표도 지운다")
  void resetClearsVotes() throws Exception {
    vote(guest("v17"), projectA.getId(), PHONE, true).andExpect(status().isOk());
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();

    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", adminBearer()))
        .andExpect(status().isOk());

    org.assertj.core.api.Assertions.assertThat(voteRepository.count()).isZero();
    org.assertj.core.api.Assertions.assertThat(investorRepository.count()).isZero();
  }
}
