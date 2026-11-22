package com.getit.global.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.crypto.PhoneNumberMasker;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 어드민 응답의 번호 가림, 전체 번호 단건 조회, 열람 · 내려받기 기록. (이슈 #237) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPhoneAccessTest {

  private static final String PHONE = "010-1234-5678";
  private static final String MASKED = "010-****-5678";

  @Autowired private MockMvc mockMvc;
  @Autowired private JwtProvider jwtProvider;
  @Autowired private UserRepository userRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private AdminAuditLogRepository auditLogRepository;

  private User guest(String id) {
    return userRepository.saveAndFlush(
        User.createGuest(id, id + "@getit.com", "홍길동" + id, "https://cdn.getit.com/1.png"));
  }

  private User member(String id) {
    User user = guest(id);
    user.updateApplicantInfo(PHONE, "IT대학", "컴퓨터학부", 3, "2021110000");
    user.promoteToMember(9);
    return userRepository.saveAndFlush(user);
  }

  private User admin() {
    User admin = guest("phone-admin");
    admin.updateRole(Role.ADMIN);
    return userRepository.saveAndFlush(admin);
  }

  private String bearer(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  private Application submitted(String studentNumber) {
    Application application = Application.createDraft(
        1L, 1L, "지원자", "apply@getit.com", PHONE, null, null, 3, studentNumber);
    application.submit(LocalDateTime.now(), LocalDateTime.now());
    return applicationRepository.saveAndFlush(application);
  }

  private List<AdminAuditLog> logs() {
    return auditLogRepository.findAll();
  }

  @Test
  @DisplayName("부원 목록에는 가린 번호만 나온다")
  void userListMasked() throws Exception {
    member("pm1");
    mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(admin())))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(MASKED)))
        .andExpect(content().string(not(containsString(PHONE))));
  }

  @Test
  @DisplayName("부원 전체 번호는 단건으로 받고, 볼 때마다 누가 누구를 봤는지 남는다")
  void revealUserPhone() throws Exception {
    User target = member("pm2");
    User admin = admin();

    mockMvc.perform(get("/api/admin/users/{id}/phone", target.getId()).header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.phoneNumber").value(PHONE));

    assertThat(logs()).singleElement().satisfies(log -> {
      assertThat(log.getAction()).isEqualTo(AdminAuditAction.VIEW_USER_PHONE);
      assertThat(log.getAdminId()).isEqualTo(admin.getId());
      assertThat(log.getTargetId()).isEqualTo(target.getId());
    });
  }

  @Test
  @DisplayName("없는 사용자는 404 이고 기록하지 않는다")
  void revealUnknownUser() throws Exception {
    mockMvc.perform(get("/api/admin/users/{id}/phone", 999_999L).header("Authorization", bearer(admin())))
        .andExpect(status().isNotFound());
    assertThat(logs()).isEmpty();
  }

  @Test
  @DisplayName("지원자 상세에는 가린 번호, 단건 조회로 전체 번호가 나오고 기록된다")
  void revealApplicantPhone() throws Exception {
    Application application = submitted("2021110001");
    User admin = admin();

    mockMvc.perform(get("/api/admin/recruitment/applications/{id}", application.getId())
            .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.basicInfo.phoneNumber").value(MASKED));
    mockMvc.perform(get("/api/admin/recruitment/applications/{id}/phone", application.getId())
            .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(jsonPath("$.data.phoneNumber").value(PHONE));

    assertThat(logs()).singleElement().satisfies(log -> {
      assertThat(log.getAction()).isEqualTo(AdminAuditAction.VIEW_APPLICANT_PHONE);
      assertThat(log.getTargetId()).isEqualTo(application.getId());
    });
  }

  @Test
  @DisplayName("전체 번호 단건 조회는 ADMIN 만")
  void adminOnly() throws Exception {
    User target = member("pm3");
    Application application = submitted("2021110002");
    String memberBearer = bearer(target);

    mockMvc.perform(get("/api/admin/users/{id}/phone", target.getId()).header("Authorization", memberBearer))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/admin/recruitment/applications/{id}/phone", application.getId())
            .header("Authorization", memberBearer))
        .andExpect(status().isForbidden());
    assertThat(logs()).isEmpty();
  }

  @Test
  @DisplayName("부원 엑셀은 기본으로 연락처를 빼고, includePhone=true 면 넣는다. 둘 다 기록된다")
  void exportExcludesPhoneByDefault() throws Exception {
    member("pm4");
    User admin = admin();

    byte[] withoutPhone = mockMvc.perform(get("/api/admin/users/export").header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andExpect(header().string("Cache-Control", "no-store"))
        .andReturn().getResponse().getContentAsByteArray();
    byte[] withPhone = mockMvc.perform(get("/api/admin/users/export").param("includePhone", "true")
            .header("Authorization", bearer(admin)))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsByteArray();

    assertThat(sheetText(withoutPhone)).doesNotContain("연락처").doesNotContain(PHONE);
    assertThat(sheetText(withPhone)).contains("연락처").contains(PHONE);
    assertThat(logs()).extracting(AdminAuditLog::getAction)
        .containsExactly(AdminAuditAction.EXPORT_USERS, AdminAuditAction.EXPORT_USERS_WITH_PHONE);
  }

  private static String sheetText(byte[] excel) throws Exception {
    StringBuilder text = new StringBuilder();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
      for (Row row : workbook.getSheetAt(0)) {
        row.forEach(cell -> text.append(cell.toString()).append('|'));
      }
    }
    return text.toString();
  }

  @Test
  @DisplayName("가림: 앞 3자리와 끝 4자리 숫자만 남기고 구분자는 그대로 둔다")
  void masker() {
    assertThat(PhoneNumberMasker.mask(PHONE)).isEqualTo(MASKED);
    assertThat(PhoneNumberMasker.mask("01012345678")).isEqualTo("010****5678");
    assertThat(PhoneNumberMasker.mask("010-123-4567")).isEqualTo("010-***-4567");
    assertThat(PhoneNumberMasker.mask("1234567")).isEqualTo("*******");
    assertThat(PhoneNumberMasker.mask(null)).isNull();
  }
}
