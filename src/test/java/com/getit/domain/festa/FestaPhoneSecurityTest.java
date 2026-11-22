package com.getit.domain.festa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.festa.config.FestaProperties;
import com.getit.domain.festa.entity.FestaAuditAction;
import com.getit.domain.festa.entity.FestaAuditLog;
import com.getit.domain.festa.entity.FestaSchedule;
import com.getit.domain.festa.repository.FestaAuditLogRepository;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.festa.repository.FestaScheduleRepository;
import com.getit.domain.festa.security.PhoneNumberCipher;
import com.getit.domain.festa.security.PlainPhoneNumberEncryptor;
import com.getit.domain.festa.service.FestaAdminService;
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
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
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

/** 추첨용 전화번호 보호: 암호화 저장, 응답 비노출, 종료 뒤 내려받기, 감사 기록, 자동 파기. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FestaPhoneSecurityTest {

  private static final String PHONE = "010-1234-5678";
  private static final String ADMIN = "/api/admin/festa";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private FeatureToggleRepository featureToggleRepository;
  @Autowired private FestaInvestorRepository investorRepository;
  @Autowired private FestaScheduleRepository scheduleRepository;
  @Autowired private FestaAuditLogRepository auditLogRepository;
  @Autowired private FestaAdminService festaAdminService;
  @Autowired private PlainPhoneNumberEncryptor plainPhoneNumberEncryptor;
  @Autowired private PhoneNumberCipher cipher;
  @Autowired private EntityManager entityManager;

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
          User created = guest("sec-admin");
          created.updateRole(Role.ADMIN);
          return userRepository.saveAndFlush(created);
        });
  }

  private String adminBearer() {
    User admin = admin();
    return "Bearer " + jwtProvider.createAccessToken(admin.getId(), admin.getEmail(), Role.ADMIN);
  }

  private ResultActions vote(User user, String phone) throws Exception {
    return mockMvc.perform(put("/api/festa/vote")
        .header("Authorization", bearer(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"projectId\":%d,\"phoneNumber\":\"%s\",\"privacyConsent\":true}".formatted(project.getId(), phone)));
  }

  private String rawPhone(Long userId) {
    Long id = investorRepository.findByUserId(userId).orElseThrow().getId();
    return investorRepository.findRawPhoneNumber(id);
  }

  private void closeToggle() {
    featureToggleRepository.findById(FeatureKey.MOCK_INVESTMENT).orElseThrow().updateEnabled(false, 1L);
    featureToggleRepository.flush();
  }

  private static LocalDateTime now() {
    return LocalDateTime.now(ZoneId.of("Asia/Seoul"));
  }

  @Test
  @DisplayName("DB 에는 암호문만 남고, 같은 번호도 저장값이 매번 다르다")
  void storesCiphertext() throws Exception {
    User a = guest("p1");
    User b = guest("p2");
    vote(a, PHONE).andExpect(status().isOk());
    vote(b, PHONE).andExpect(status().isOk());

    String rawA = rawPhone(a.getId());
    String rawB = rawPhone(b.getId());
    assertThat(rawA).startsWith("v1:").doesNotContain("1234").doesNotContain("5678");
    assertThat(rawA).isNotEqualTo(rawB);
    assertThat(cipher.decrypt(rawA)).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("투표·투자 응답과 에러 응답에 원번호가 나오지 않는다")
  void responsesNeverContainFullNumber() throws Exception {
    User user = guest("p3");
    vote(user, PHONE).andExpect(content().string(not(containsString(PHONE))));
    mockMvc.perform(get("/api/festa/vote").header("Authorization", bearer(user)))
        .andExpect(content().string(not(containsString(PHONE))));
    mockMvc.perform(get("/api/festa/portfolio").header("Authorization", bearer(user)))
        .andExpect(content().string(not(containsString(PHONE))));

    vote(guest("p4"), "010-1234-56789")
        .andExpect(status().isBadRequest())
        .andExpect(content().string(not(containsString("010-1234-56789"))));
  }

  @Test
  @DisplayName("다른 키로는 풀리지 않는다")
  void wrongKeyFails() {
    String stored = cipher.encrypt(PHONE);
    PhoneNumberCipher other = new PhoneNumberCipher(
        new FestaProperties("b3RoZXIta2V5LW90aGVyLWtleS1vdGhlci1rZXkhISE=", 30));
    assertThatThrownBy(() -> other.decrypt(stored))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining(stored);
  }

  @Test
  @DisplayName("키가 없거나 32바이트가 아니면 기동하지 않는다")
  void rejectsBadKey() {
    assertThatThrownBy(() -> new PhoneNumberCipher(new FestaProperties(null, 30)))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> new PhoneNumberCipher(new FestaProperties("c2hvcnQ=", 30)))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("이전에 평문으로 저장된 번호는 기동 시 암호화되고, 그대로 읽힌다")
  void encryptsLegacyPlainRows() throws Exception {
    User user = guest("p5");
    vote(user, PHONE).andExpect(status().isOk());
    Long id = investorRepository.findByUserId(user.getId()).orElseThrow().getId();
    investorRepository.updateRawPhoneNumber(id, PHONE);
    assertThat(investorRepository.findPlainPhoneNumberIds()).contains(id);

    plainPhoneNumberEncryptor.encryptPlainRows();

    assertThat(investorRepository.findRawPhoneNumber(id)).startsWith("v1:");
    assertThat(investorRepository.findPlainPhoneNumberIds()).isEmpty();
    mockMvc.perform(get("/api/festa/vote").header("Authorization", bearer(user)))
        .andExpect(jsonPath("$.data.maskedPhoneNumber").value("010-****-5678"));
  }

  @Test
  @DisplayName("투표 중에는 명단을 내려받을 수 없다 (일정 없으면 토글 기준)")
  void exportBlockedWhileOpen() throws Exception {
    vote(guest("p6"), PHONE).andExpect(status().isOk());
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("FESTA_EXPORT_BEFORE_END"));
  }

  @Test
  @DisplayName("일정이 있으면 종료 전에는 토글을 꺼도 내려받을 수 없다")
  void exportBlockedBeforeScheduleEnd() throws Exception {
    scheduleRepository.saveAndFlush(FestaSchedule.of(now().minusDays(1), now().plusDays(1)));
    closeToggle();
    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("종료 뒤 내려받으면 번호는 가려서 나오고, 캐시 금지와 감사 기록이 남는다")
  void exportAfterEndAudited() throws Exception {
    vote(guest("p7"), PHONE).andExpect(status().isOk());
    scheduleRepository.saveAndFlush(FestaSchedule.of(now().minusDays(2), now().minusMinutes(1)));

    mockMvc.perform(get(ADMIN + "/investors.csv").header("Authorization", adminBearer()))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(content().string(containsString("010-****-5678")))
        .andExpect(content().string(not(containsString(PHONE))));

    List<FestaAuditLog> logs = auditLogRepository.findAll();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getAction()).isEqualTo(FestaAuditAction.EXPORT_PARTICIPANTS);
    assertThat(logs.get(0).getAdminId()).isEqualTo(admin().getId());
    assertThat(logs.get(0).getItemCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("수동 파기와 초기화도 감사 기록이 남는다")
  void destroyAndResetAudited() throws Exception {
    vote(guest("p8"), PHONE).andExpect(status().isOk());
    mockMvc.perform(delete(ADMIN + "/investors/phones").header("Authorization", adminBearer()))
        .andExpect(status().isOk());
    closeToggle();
    mockMvc.perform(delete(ADMIN + "/investments").header("Authorization", adminBearer()))
        .andExpect(status().isOk());

    assertThat(auditLogRepository.findAll()).extracting(FestaAuditLog::getAction)
        .containsExactlyInAnyOrder(FestaAuditAction.DESTROY_PHONES, FestaAuditAction.RESET_ALL);
  }

  @Test
  @DisplayName("자동 파기: 종료 뒤 보관 기간이 지나야만 지운다")
  void autoDestroyAfterRetention() throws Exception {
    User user = guest("p9");
    vote(user, PHONE).andExpect(status().isOk());

    festaAdminService.destroyPhoneNumbersIfExpired();
    assertThat(investorRepository.existsByPhoneNumberIsNotNull()).as("일정 없으면 건드리지 않는다").isTrue();

    scheduleRepository.saveAndFlush(FestaSchedule.of(now().minusDays(40), now().minusDays(29)));
    festaAdminService.destroyPhoneNumbersIfExpired();
    assertThat(investorRepository.existsByPhoneNumberIsNotNull()).as("29일째는 남긴다").isTrue();

    scheduleRepository.findById(FestaSchedule.SINGLETON_ID).orElseThrow()
        .change(now().minusDays(40), now().minusDays(31));
    scheduleRepository.flush();
    festaAdminService.destroyPhoneNumbersIfExpired();
    entityManager.clear();
    assertThat(investorRepository.existsByPhoneNumberIsNotNull()).isFalse();
    assertThat(auditLogRepository.findAll()).extracting(FestaAuditLog::getAction)
        .containsExactly(FestaAuditAction.AUTO_DESTROY_PHONES);

    festaAdminService.destroyPhoneNumbersIfExpired();
    assertThat(auditLogRepository.count()).as("지울 게 없으면 기록하지 않는다").isEqualTo(1);
  }
}
