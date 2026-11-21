package com.getit.domain.festa.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.festa.repository.FestaScheduleRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

/** FESTA 투표 기간 (이슈 #234) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FestaScheduleControllerTest {

  private static final String SCHEDULE = "/api/admin/festa/schedule";
  private static final String PHONE = "010-1234-5678";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private FeatureToggleRepository featureToggleRepository;
  @Autowired private FestaScheduleRepository scheduleRepository;

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

  private String adminBearer() {
    User admin = userRepository.findAll().stream()
        .filter(user -> user.getRole() == Role.ADMIN)
        .findFirst()
        .orElseGet(() -> {
          User created = guest("schedule-admin");
          created.updateRole(Role.ADMIN);
          return userRepository.saveAndFlush(created);
        });
    return "Bearer " + jwtProvider.createAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private ResultActions saveSchedule(OffsetDateTime start, OffsetDateTime end) throws Exception {
    return mockMvc.perform(put(SCHEDULE)
        .header("Authorization", adminBearer())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"startAt\":\"%s\",\"endAt\":\"%s\"}".formatted(start, end)));
  }

  private ResultActions vote(User user) throws Exception {
    return mockMvc.perform(put("/api/festa/vote")
        .header("Authorization", bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"projectId\":%d,\"phoneNumber\":\"%s\",\"privacyConsent\":true}".formatted(project.getId(), PHONE)));
  }

  private ResultActions invest(User user) throws Exception {
    return mockMvc.perform(put("/api/festa/portfolio")
        .header("Authorization", bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"phoneNumber\":\"%s\",\"privacyConsent\":true,\"investments\":[{\"projectId\":%d,\"amount\":100000}]}"
            .formatted(PHONE, project.getId())));
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  @Test
  @DisplayName("미설정이면 둘 다 null 이다")
  void unsetIsNull() throws Exception {
    mockMvc.perform(get(SCHEDULE).header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.startAt").value(nullValue()))
        .andExpect(jsonPath("$.data.endAt").value(nullValue()));
  }

  @Test
  @DisplayName("저장하고 다시 읽으면 같은 시각이 나온다. 다시 저장하면 덮어쓴다")
  void saveAndRead() throws Exception {
    OffsetDateTime start = OffsetDateTime.parse("2026-11-23T10:00:00+09:00");
    OffsetDateTime end = OffsetDateTime.parse("2026-11-29T18:00:00+09:00");
    saveSchedule(start, end).andExpect(status().isOk());
    saveSchedule(start, end.plusDays(1)).andExpect(status().isOk());

    mockMvc.perform(get(SCHEDULE).header("Authorization", adminBearer()))
        .andExpect(jsonPath("$.data.startAt").value("2026-11-23T10:00:00+09:00"))
        .andExpect(jsonPath("$.data.endAt").value("2026-11-30T18:00:00+09:00"));
    org.assertj.core.api.Assertions.assertThat(scheduleRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("startAt 이 endAt 보다 앞서지 않으면 400 이다")
  void rejectsInvertedRange() throws Exception {
    OffsetDateTime t = now();
    saveSchedule(t, t).andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_FESTA_SCHEDULE"));
    saveSchedule(t.plusDays(1), t).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("일정 관리는 어드민만 한다")
  void adminOnly() throws Exception {
    mockMvc.perform(get(SCHEDULE).header("Authorization", bearer(guest("s1"))))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("일정이 없으면 토글만으로 투표와 투자가 된다")
  void noScheduleFollowsToggle() throws Exception {
    vote(guest("s2")).andExpect(status().isOk());
    invest(guest("s3")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("기간 안이면 투표와 투자가 된다")
  void withinSchedule() throws Exception {
    saveSchedule(now().minusHours(1), now().plusHours(1)).andExpect(status().isOk());
    vote(guest("s4")).andExpect(status().isOk());
    invest(guest("s5")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("시작 전과 마감 뒤에는 토글이 켜져 있어도 FESTA_CLOSED 이고, 조회는 열려 있다")
  void outsideScheduleIsClosed() throws Exception {
    User guest = guest("s6");
    saveSchedule(now().plusHours(1), now().plusHours(2)).andExpect(status().isOk());
    vote(guest).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("FESTA_CLOSED"));
    invest(guest).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("FESTA_CLOSED"));

    saveSchedule(now().minusHours(2), now().minusHours(1)).andExpect(status().isOk());
    vote(guest).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("FESTA_CLOSED"));
    mockMvc.perform(get("/api/festa/vote").header("Authorization", bearer(guest)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("홈 응답에 festaSchedule 이 내려오고, 미설정이면 null 이다")
  void homeIncludesSchedule() throws Exception {
    mockMvc.perform(get("/api/public/home"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.festaSchedule").value(nullValue()));

    saveSchedule(OffsetDateTime.parse("2026-11-23T10:00:00+09:00"), OffsetDateTime.parse("2026-11-29T18:00:00+09:00"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/public/home"))
        .andExpect(jsonPath("$.data.festaSchedule.startAt").value("2026-11-23T10:00:00+09:00"))
        .andExpect(jsonPath("$.data.festaSchedule.endAt").value("2026-11-29T18:00:00+09:00"));
  }
}
