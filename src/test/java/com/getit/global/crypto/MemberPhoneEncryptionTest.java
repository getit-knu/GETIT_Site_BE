package com.getit.global.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 부원 · 지원서 전화번호 암호화: 저장, 읽기, 일괄 UPDATE, 기존 평문, 키 교체. (이슈 #239) */
@SpringBootTest
@Transactional
class MemberPhoneEncryptionTest {

  private static final String PHONE = "010-1234-5678";
  /** test application.yml 의 키. */
  private static final String TEST_KEY = "dGVzdC1mZXN0YS1waG9uZS1rZXktMzItYnl0ZXMhISE=";
  private static final String NEW_KEY = "b3RoZXIta2V5LW90aGVyLWtleS1vdGhlci1rZXkhISE=";

  @Autowired private UserRepository userRepository;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private PlainPhoneNumberEncryptor encryptor;
  @Autowired private PhoneNumberCipher cipher;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private EntityManager entityManager;

  private User userWithPhone(String id) {
    User user = User.createGuest(id, id + "@getit.com", "홍길동", "https://cdn.getit.com/1.png");
    user.editProfile("홍길동", PHONE, "https://cdn.getit.com/1.png", null);
    return userRepository.saveAndFlush(user);
  }

  private String raw(String table, Long id) {
    entityManager.flush();
    return jdbcTemplate.queryForObject("select phone_number from " + table + " where id = ?", String.class, id);
  }

  private void writeRaw(String table, Long id, String stored) {
    entityManager.flush();
    jdbcTemplate.update("update " + table + " set phone_number = ? where id = ?", stored, id);
    entityManager.clear();
  }

  @Test
  @DisplayName("부원 번호는 DB 에 암호문으로, 엔티티에서는 원래 번호로 보인다")
  void userPhoneEncrypted() {
    User user = userWithPhone("enc-u1");

    assertThat(raw("users", user.getId())).startsWith("v1:").doesNotContain("5678");
    entityManager.clear();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getPhoneNumber()).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("지원서 번호도 암호문으로 저장된다")
  void applicationPhoneEncrypted() {
    Application application = applicationRepository.saveAndFlush(Application.createDraft(
        1L, 1L, "홍길동", "a@getit.com", PHONE, null, null, 3, "2021110000"));

    assertThat(raw("application", application.getId())).startsWith("v1:").doesNotContain("5678");
    entityManager.clear();
    assertThat(applicationRepository.findById(application.getId()).orElseThrow().getPhoneNumber()).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("승격의 일괄 UPDATE 로 넣은 번호도 암호화된다")
  void bulkUpdateEncrypts() {
    User guest = userRepository.saveAndFlush(
        User.createGuest("enc-u2", "enc-u2@getit.com", "홍길동", "https://cdn.getit.com/1.png"));

    int updated = userRepository.promoteIfEligible(
        guest.getId(), Role.MEMBER, Role.GUEST, 9, PHONE, null, null, 3, "2021110000");

    assertThat(updated).isEqualTo(1);
    assertThat(raw("users", guest.getId())).startsWith("v1:");
    assertThat(userRepository.findById(guest.getId()).orElseThrow().getPhoneNumber()).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("기존 평문 번호는 기동 때 세 테이블 모두 암호화되고 그대로 읽힌다")
  void encryptsLegacyPlainRows() {
    User user = userWithPhone("enc-u3");
    Application application = applicationRepository.saveAndFlush(Application.createDraft(
        1L, 2L, "홍길동", "b@getit.com", PHONE, null, null, 3, "2021110001"));
    writeRaw("users", user.getId(), PHONE);
    writeRaw("application", application.getId(), PHONE);
    assertThat(userRepository.findById(user.getId()).orElseThrow().getPhoneNumber()).isEqualTo(PHONE);

    encryptor.encryptPlainRows();

    assertThat(raw("users", user.getId())).startsWith("v1:");
    assertThat(raw("application", application.getId())).startsWith("v1:");
    entityManager.clear();
    assertThat(userRepository.findById(user.getId()).orElseThrow().getPhoneNumber()).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("키 교체: 이전 키로 된 값을 읽고, 새 키로 다시 암호화한다")
  void rotatesKey() {
    String oldStored = cipher.encrypt(PHONE);
    PhoneNumberCipher rotating = new PhoneNumberCipher(NEW_KEY, TEST_KEY);
    PhoneNumberCipher newOnly = new PhoneNumberCipher(NEW_KEY, null);

    assertThat(rotating.rotating()).isTrue();
    assertThat(rotating.decrypt(oldStored)).isEqualTo(PHONE);
    assertThat(rotating.needsEncryption(oldStored)).isTrue();
    assertThatThrownBy(() -> newOnly.decrypt(oldStored)).isInstanceOf(IllegalStateException.class);

    String reEncrypted = rotating.encrypt(rotating.decrypt(oldStored));
    assertThat(rotating.needsEncryption(reEncrypted)).isFalse();
    assertThat(newOnly.decrypt(reEncrypted)).isEqualTo(PHONE);
  }

  @Test
  @DisplayName("이전 키 형식이 틀려도 기동하지 않는다")
  void rejectsBadPreviousKey() {
    assertThatThrownBy(() -> new PhoneNumberCipher(NEW_KEY, "c2hvcnQ="))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("FESTA_PHONE_KEY_PREVIOUS");
  }
}
