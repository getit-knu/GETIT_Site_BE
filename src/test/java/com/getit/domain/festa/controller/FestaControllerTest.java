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
import com.getit.domain.festa.repository.FestaInvestmentRepository;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
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
import org.springframework.transaction.annotation.Transactional;

/** 모의투자 FESTA 가상 투자 (이슈 #222) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FestaControllerTest {

  private static final String PORTFOLIO = "/api/festa/portfolio";
  private static final String ADMIN = "/api/admin/festa";
  private static final String PHONE = "010-1234-5678";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private GroupRepository groupRepository;
  @Autowired private GenerationRepository generationRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private FeatureToggleRepository featureToggleRepository;
  @Autowired private FestaInvestmentRepository investmentRepository;

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
    project.changeStatus(com.getit.domain.project.entity.ProjectStatus.APPROVED);
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
          User created = guest("festa-admin");
          created.updateRole(Role.ADMIN);
          return userRepository.saveAndFlush(created);
        });
    return "Bearer " + jwtProvider.createAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private String body(String phone, Boolean consent, long... idAndAmount) {
    StringBuilder items = new StringBuilder();
    for (int i = 0; i < idAndAmount.length; i += 2) {
      if (i > 0) {
        items.append(',');
      }
      items.append("{\"projectId\":%d,\"amount\":%d}".formatted(idAndAmount[i], idAndAmount[i + 1]));
    }
    return "{\"phoneNumber\":%s,\"privacyConsent\":%s,\"investments\":[%s]}".formatted(
        phone == null ? "null" : "\"" + phone + "\"", consent, items);
  }

  private org.springframework.test.web.servlet.ResultActions save(User user, String body) throws Exception {
    return mockMvc.perform(put(PORTFOLIO)
        .header("Authorization", bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  @Test
  @DisplayName("로그인 없이는 401 이다")
  void requiresLogin() throws Exception {
    mockMvc.perform(get(PORTFOLIO)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GUEST 도 투자하고, 번호는 마스킹되어 돌아온다")
  void guestInvests() throws Exception {
    User guest = guest("g1");

    save(guest, body(PHONE, true, projectA.getId(), 3_000_000, projectB.getId(), 2_000_000))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.budget").value(10_000_000))
        .andExpect(jsonPath("$.data.remaining").value(5_000_000))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"))
        .andExpect(jsonPath("$.data.investments.length()").value(2));

    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.remaining").value(5_000_000));
  }

  @Test
  @DisplayName("투자 전에는 maskedPhoneNumber 가 null 이다")
  void maskedPhoneIsNullBeforeInvesting() throws Exception {
    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest("g2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value(nullValue()))
        .andExpect(jsonPath("$.data.remaining").value(10_000_000));
  }

  @Test
  @DisplayName("다시 저장하면 기존 투자를 통째로 바꾸고, 번호를 안 보내면 저장된 번호를 유지한다")
  void replacesAndKeepsPhone() throws Exception {
    User guest = guest("g3");
    save(guest, body(PHONE, true, projectA.getId(), 1_000_000)).andExpect(status().isOk());

    save(guest, body(null, null, projectB.getId(), 500_000, projectA.getId(), 0))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.investments.length()").value(1))
        .andExpect(jsonPath("$.data.investments[0].projectId").value(projectB.getId()))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"));
  }

  @Test
  @DisplayName("토글이 꺼지면 저장은 막히고 조회는 된다")
  void closedWhenToggleOff() throws Exception {
    User guest = guest("g4");
    save(guest, body(PHONE, true, projectA.getId(), 1_000_000)).andExpect(status().isOk());
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();

    save(guest, body(null, null, projectA.getId(), 2_000_000))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_CLOSED"));
    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.investments[0].amount").value(1_000_000));
  }

  @Test
  @DisplayName("출전하지 않은 프로젝트는 거부한다")
  void rejectsNotEntered() throws Exception {
    save(guest("g5"), body(PHONE, true, notEntered.getId(), 100_000))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PROJECT_NOT_ENTERED"));
  }

  @Test
  @DisplayName("부원은 자기 조 프로젝트에 투자할 수 없다")
  void rejectsOwnGroupProject() throws Exception {
    save(memberOfMyGroup("m1"), body(PHONE, true, myGroupProject.getId(), 100_000))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("OWN_GROUP_PROJECT"));
  }

  @Test
  @DisplayName("다른 조 부원은 투자할 수 있다")
  void otherGroupMemberCanInvest() throws Exception {
    save(memberOfMyGroup("m2"), body(PHONE, true, projectA.getId(), 100_000)).andExpect(status().isOk());
  }

  @Test
  @DisplayName("금액 규칙: 합계 초과 · 음수 · 10만 원 단위 아님 · 중복 프로젝트")
  void rejectsBadAmounts() throws Exception {
    User guest = guest("g6");
    save(guest, body(PHONE, true, projectA.getId(), 10_000_000, projectB.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("BUDGET_EXCEEDED"));
    save(guest, body(PHONE, true, projectA.getId(), -100_000))
        .andExpect(jsonPath("$.error.code").value("INVALID_AMOUNT"));
    save(guest, body(PHONE, true, projectA.getId(), 150_000))
        .andExpect(jsonPath("$.error.code").value("INVALID_AMOUNT"));
    save(guest, body(PHONE, true, projectA.getId(), 100_000, projectA.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("DUPLICATE_PROJECT"));
  }

  @Test
  @DisplayName("전화번호: 첫 저장엔 필수, 형식 오류, 동의 없음")
  void rejectsBadPhone() throws Exception {
    User guest = guest("g7");
    save(guest, body(null, null, projectA.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("PHONE_NUMBER_REQUIRED"));
    save(guest, body("01012345678", true, projectA.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("INVALID_PHONE_NUMBER"));
    save(guest, body(PHONE, false, projectA.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("PRIVACY_CONSENT_REQUIRED"));
    save(guest, body(PHONE, null, projectA.getId(), 100_000))
        .andExpect(jsonPath("$.error.code").value("PRIVACY_CONSENT_REQUIRED"));
  }

  @Test
  @DisplayName("거부된 요청은 기존 투자를 건드리지 않는다")
  void rejectionKeepsExisting() throws Exception {
    User guest = guest("g8");
    save(guest, body(PHONE, true, projectA.getId(), 1_000_000)).andExpect(status().isOk());

    save(guest, body(null, null, projectA.getId(), 150_000)).andExpect(status().isBadRequest());

    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest)))
        .andExpect(jsonPath("$.data.investments[0].amount").value(1_000_000));
  }

  @Test
  @DisplayName("어드민 집계: 총액 내림차순, 투자자 수, 투자 없는 출전 프로젝트는 0")
  void adminResults() throws Exception {
    save(guest("r1"), body(PHONE, true, projectA.getId(), 1_000_000, projectB.getId(), 3_000_000));
    save(guest("r2"), body(PHONE, true, projectA.getId(), 1_000_000));

    mockMvc.perform(get(ADMIN + "/results").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.investorCount").value(2))
        .andExpect(jsonPath("$.data.projects.length()").value(3))
        .andExpect(jsonPath("$.data.projects[0].title").value("B"))
        .andExpect(jsonPath("$.data.projects[0].totalAmount").value(3_000_000))
        .andExpect(jsonPath("$.data.projects[1].title").value("A"))
        .andExpect(jsonPath("$.data.projects[1].totalAmount").value(2_000_000))
        .andExpect(jsonPath("$.data.projects[1].investorCount").value(2))
        .andExpect(jsonPath("$.data.projects[2].totalAmount").value(0));
  }

  @Test
  @DisplayName("어드민이 아니면 집계 · CSV · 파기가 403 이다")
  void adminOnly() throws Exception {
    String member = bearer(memberOfMyGroup("m3"));
    mockMvc.perform(get(ADMIN + "/results").header("Authorization", member)).andExpect(status().isForbidden());
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", member)).andExpect(status().isForbidden());
    mockMvc.perform(delete(ADMIN + "/investors/phones").header("Authorization", member))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("CSV: 투자한 사람만, 전부 0 으로 돌린 사람은 뺀다")
  void csvExcludesZeroedInvestors() throws Exception {
    User invested = guest("c1");
    User zeroed = guest("c2");
    save(invested, body(PHONE, true, projectA.getId(), 100_000));
    save(zeroed, body("010-9999-0000", true, projectA.getId(), 100_000));
    save(zeroed, body(null, null, projectA.getId(), 0));

    closeFesta();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/csv"))
        .andExpect(content().string(containsString(PHONE)))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString("010-9999-0000"))));
  }

  @Test
  @DisplayName("CSV: 수식으로 읽히는 이름은 무력화한다")
  void csvNeutralizesFormula() throws Exception {
    User evil = userRepository.saveAndFlush(
        User.createGuest("c3", "c3@getit.com", "=HYPERLINK(\"x\")", "https://cdn.getit.com/1.png"));
    save(evil, body(PHONE, true, projectA.getId(), 100_000));

    closeFesta();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(content().string(containsString("\"'=HYPERLINK(\"\"x\"\")\"")));
  }

  @Test
  @DisplayName("번호 파기: 번호와 동의만 지우고 투자 내역과 집계는 남긴다")
  void destroysPhonesKeepsInvestments() throws Exception {
    User guest = guest("d1");
    save(guest, body(PHONE, true, projectA.getId(), 1_000_000));

    mockMvc.perform(delete(ADMIN + "/investors/phones").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.destroyedCount").value(1));

    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest)))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value(nullValue()))
        .andExpect(jsonPath("$.data.investments[0].amount").value(1_000_000));
    mockMvc.perform(get(ADMIN + "/results").header("Authorization", adminBearer()))
        .andExpect(jsonPath("$.data.projects[0].totalAmount").value(1_000_000));
    closeFesta();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString(PHONE))));
  }

  @Test
  @DisplayName("초기화: 토글이 켜져 있으면 거부하고 데이터는 그대로다 (이슈 #229)")
  void resetRejectedWhileOpen() throws Exception {
    save(guest("x1"), body(PHONE, true, projectA.getId(), 1_000_000)).andExpect(status().isOk());

    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", adminBearer()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_RESET_WHILE_OPEN"));

    mockMvc.perform(get(ADMIN + "/results").header("Authorization", adminBearer()))
        .andExpect(jsonPath("$.data.investorCount").value(1));
  }

  @Test
  @DisplayName("초기화: 토글이 꺼져 있으면 투자 내역과 전화번호를 모두 지운다 (이슈 #229)")
  void resetClearsEverythingWhenClosed() throws Exception {
    User first = guest("x2");
    save(first, body(PHONE, true, projectA.getId(), 1_000_000)).andExpect(status().isOk());
    save(guest("x3"), body("010-2222-3333", true, projectB.getId(), 500_000)).andExpect(status().isOk());
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();

    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.deletedInvestorCount").value(2));

    mockMvc.perform(get(ADMIN + "/results").header("Authorization", adminBearer()))
        .andExpect(jsonPath("$.data.investorCount").value(0))
        .andExpect(jsonPath("$.data.projects[0].totalAmount").value(0));
    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(first)))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value(nullValue()))
        .andExpect(jsonPath("$.data.investments.length()").value(0));
  }

  @Test
  @DisplayName("초기화는 어드민만 할 수 있다 (이슈 #229)")
  void resetAdminOnly() throws Exception {
    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", bearer(memberOfMyGroup("x4"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("어드민이 조를 지정해 만든 출전 프로젝트에도 우리 조 투자 금지가 걸린다 (이슈 #228)")
  void adminCreatedProjectBlocksOwnGroup() throws Exception {
    Project adminCreated = Project.create(new ProjectCommand(
        "어드민 등록", "팀", "2026-FALL", "설명", List.of(), null, null, false, null), 9);
    adminCreated.assignGroup(myGroup.getId());
    adminCreated.updateFesta(true, 9);
    projectRepository.saveAndFlush(adminCreated);

    save(memberOfMyGroup("o1"), body(PHONE, true, adminCreated.getId(), 100_000))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.error.code").value("OWN_GROUP_PROJECT"));
  }

  @Test
  @DisplayName("포트폴리오 응답에 내 조의 출전 프로젝트 id 를 싣는다 (이슈 #228)")
  void listsOwnProjectIds() throws Exception {
    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(memberOfMyGroup("o2"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.ownProjectIds.length()").value(1))
        .andExpect(jsonPath("$.data.ownProjectIds[0]").value(myGroupProject.getId()));

    mockMvc.perform(get(PORTFOLIO).header("Authorization", bearer(guest("o3"))))
        .andExpect(jsonPath("$.data.ownProjectIds.length()").value(0));
  }
}
