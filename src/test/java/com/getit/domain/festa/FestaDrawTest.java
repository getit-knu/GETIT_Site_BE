package com.getit.domain.festa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.festa.entity.FestaAuditAction;
import com.getit.domain.festa.entity.FestaAuditLog;
import com.getit.domain.festa.entity.FestaSchedule;
import com.getit.domain.festa.entity.FestaWinner;
import com.getit.domain.festa.repository.FestaAuditLogRepository;
import com.getit.domain.festa.repository.FestaScheduleRepository;
import com.getit.domain.festa.repository.FestaWinnerRepository;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

/** 서버 경품 추첨: 종료 뒤에만, 덮어쓰지 않음, 추가 추첨, 조회 기록. (이슈 #238) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FestaDrawTest {

  private static final String ADMIN = "/api/admin/festa";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private FeatureToggleRepository featureToggleRepository;
  @Autowired private FestaScheduleRepository scheduleRepository;
  @Autowired private FestaWinnerRepository winnerRepository;
  @Autowired private FestaAuditLogRepository auditLogRepository;

  private Project project;

  @BeforeEach
  void setUp() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, true));
    Project p = Project.submit(new ProjectCommand(
        "A", "팀", "2026-FALL", "설명", List.of(), null, null, false, null), 1, null);
    p.changeStatus(ProjectStatus.APPROVED);
    p.updateFesta(true, 1);
    project = projectRepository.saveAndFlush(p);
  }

  private User guest(String id) {
    return userRepository.saveAndFlush(
        User.createGuest(id, id + "@getit.com", "홍길동" + id, "https://cdn.getit.com/1.png"));
  }

  private String bearer(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  private User admin() {
    return userRepository.findAll().stream()
        .filter(user -> user.getRole() == Role.ADMIN)
        .findFirst()
        .orElseGet(() -> {
          User created = guest("draw-admin");
          created.updateRole(Role.ADMIN);
          return userRepository.saveAndFlush(created);
        });
  }

  private String adminBearer() {
    User admin = admin();
    return "Bearer " + jwtProvider.createAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  /** 투표 기간 중에 n 명이 번호를 내고 투표한 뒤, 기간을 끝낸다. */
  private void votersThenEnd(int n) throws Exception {
    for (int i = 0; i < n; i++) {
      User user = guest("dv" + i);
      mockMvc.perform(put("/api/festa/vote")
              .header("Authorization", bearer(user))
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"projectId\":%d,\"phoneNumber\":\"010-1000-%04d\",\"privacyConsent\":true}"
                  .formatted(project.getId(), i)))
          .andExpect(status().isOk());
    }
    endVoting();
  }

  private void endVoting() {
    LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    scheduleRepository.saveAndFlush(FestaSchedule.of(now.minusDays(2), now.minusMinutes(1)));
  }

  private ResultActions draw(String body) throws Exception {
    return mockMvc.perform(post(ADMIN + "/draw")
        .header("Authorization", adminBearer())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }

  private List<FestaAuditAction> auditActions() {
    return auditLogRepository.findAll().stream().map(FestaAuditLog::getAction).toList();
  }

  @Test
  @DisplayName("투표가 끝나기 전에는 추첨할 수 없다")
  void drawBlockedBeforeEnd() throws Exception {
    mockMvc.perform(put("/api/festa/vote")
            .header("Authorization", bearer(guest("early")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"projectId\":%d,\"phoneNumber\":\"010-1234-5678\",\"privacyConsent\":true}"
                .formatted(project.getId())))
        .andExpect(status().isOk());

    draw("{\"count\":1}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_DRAW_BEFORE_END"));
    assertThat(winnerRepository.count()).isZero();
  }

  @Test
  @DisplayName("요청 인원만큼 서로 다른 사람을 뽑아 저장하고, 전체 번호와 감사 기록을 남긴다")
  void drawsAndSaves() throws Exception {
    votersThenEnd(6);

    draw("{\"count\":3}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.winners", hasSize(3)))
        .andExpect(jsonPath("$.data.winners[0].drawOrder").value(1))
        .andExpect(jsonPath("$.data.winners[2].drawOrder").value(3))
        .andExpect(jsonPath("$.data.winners[0].phoneNumber").value(org.hamcrest.Matchers.matchesPattern(
            "010-1000-\\d{4}")));

    List<FestaWinner> winners = winnerRepository.findAllByOrderByDrawOrderAsc();
    assertThat(winners).hasSize(3);
    assertThat(winners.stream().map(FestaWinner::getUserId).distinct()).hasSize(3);
    assertThat(winners).allMatch(winner -> winner.getDrawnBy().equals(admin().getId()));
    assertThat(auditActions()).containsExactly(FestaAuditAction.DRAW_WINNERS);
    assertThat(auditLogRepository.findAll().get(0).getItemCount()).isEqualTo(3);
  }

  @Test
  @DisplayName("본문을 비우면 5명, 후보가 적으면 후보 전원을 뽑는다")
  void defaultCountAndFewCandidates() throws Exception {
    votersThenEnd(2);

    mockMvc.perform(post(ADMIN + "/draw").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.winners", hasSize(2)));
  }

  @Test
  @DisplayName("다시 누르면 거부하고, 추가 추첨은 이미 뽑힌 사람을 빼고 순번을 잇는다")
  void noOverwriteAndAdditional() throws Exception {
    votersThenEnd(5);
    draw("{\"count\":2}").andExpect(status().isOk());
    Set<Long> first = new HashSet<>(winnerRepository.findAll().stream().map(FestaWinner::getUserId).toList());

    draw("{\"count\":2}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_ALREADY_DRAWN"));
    assertThat(winnerRepository.count()).isEqualTo(2);

    draw("{\"count\":2,\"additional\":true}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.winners", hasSize(4)))
        .andExpect(jsonPath("$.data.winners[3].drawOrder").value(4));

    List<FestaWinner> all = winnerRepository.findAllByOrderByDrawOrderAsc();
    assertThat(all.subList(0, 2)).allMatch(winner -> first.contains(winner.getUserId()));
    assertThat(all.subList(2, 4)).noneMatch(winner -> first.contains(winner.getUserId()));
  }

  @Test
  @DisplayName("뽑을 사람이 남지 않으면 거부한다")
  void noCandidates() throws Exception {
    votersThenEnd(1);
    draw("{\"count\":1}").andExpect(status().isOk());

    draw("{\"count\":1,\"additional\":true}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_NO_DRAW_CANDIDATES"));
  }

  @Test
  @DisplayName("인원은 1~50 명")
  void countRange() throws Exception {
    votersThenEnd(1);
    draw("{\"count\":0}").andExpect(status().isBadRequest());
    draw("{\"count\":51}").andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("당첨자 조회는 같은 결과를 주고, 볼 때마다 기록하며 캐시를 막는다")
  void getWinnersAudited() throws Exception {
    votersThenEnd(3);
    draw("{\"count\":2}").andExpect(status().isOk());
    List<FestaWinner> winners = winnerRepository.findAllByOrderByDrawOrderAsc();

    mockMvc.perform(get(ADMIN + "/winners").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.winners", hasSize(2)))
        .andExpect(jsonPath("$.data.winners[0].userId").value(winners.get(0).getUserId()))
        .andExpect(jsonPath("$.data.winners[1].userId").value(winners.get(1).getUserId()));
    mockMvc.perform(get(ADMIN + "/winners").header("Authorization", adminBearer()))
        .andExpect(status().isOk());

    assertThat(auditActions()).containsExactly(
        FestaAuditAction.DRAW_WINNERS, FestaAuditAction.VIEW_WINNERS, FestaAuditAction.VIEW_WINNERS);
  }

  @Test
  @DisplayName("추첨과 당첨자 조회는 ADMIN 만")
  void adminOnly() throws Exception {
    String member = bearer(guest("not-admin"));
    mockMvc.perform(post(ADMIN + "/draw").header("Authorization", member)).andExpect(status().isForbidden());
    mockMvc.perform(get(ADMIN + "/winners").header("Authorization", member)).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("번호를 파기하면 당첨자 번호도 비고, 초기화하면 당첨자도 지운다")
  void destroyAndReset() throws Exception {
    votersThenEnd(2);
    draw("{\"count\":1}").andExpect(status().isOk());

    mockMvc.perform(delete(ADMIN + "/investors/phones").header("Authorization", adminBearer()))
        .andExpect(status().isOk());
    mockMvc.perform(get(ADMIN + "/winners").header("Authorization", adminBearer()))
        .andExpect(jsonPath("$.data.winners[0].phoneNumber").value(nullValue()));

    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();
    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", adminBearer()))
        .andExpect(status().isOk());
    assertThat(winnerRepository.count()).isZero();
  }
}
