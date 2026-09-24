package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.dto.OAuthRegistrationResult;
import com.getit.domain.user.dto.OAuthUserRegistration;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** auth 가 소비하는 사용자 계약. (작업 분할 계획 4.2) */
@SpringBootTest
@Transactional
class UserAccountServiceImplTest {

  private static final String PROVIDER_ID = "google-sub-account";
  private static final String EMAIL = "account@getit.com";

  @Autowired
  private UserAccountService userAccountService;

  @Autowired
  private UserRepository userRepository;

  private OAuthUserRegistration registration(String name, String imageUrl) {
    return new OAuthUserRegistration(PROVIDER_ID, EMAIL, name, imageUrl);
  }

  @Nested
  @DisplayName("registerOrUpdateOAuthUser")
  class RegisterOrUpdate {

    @Test
    @DisplayName("최초 로그인이면 GUEST 로 등록하고 newUser 가 true 다")
    void registersGuest() {
      OAuthRegistrationResult result =
          userAccountService.registerOrUpdateOAuthUser(registration("홍길동", "https://cdn/a.png"));

      assertThat(result.newUser()).isTrue();
      assertThat(result.account().role()).isEqualTo(Role.GUEST);
      assertThat(result.account().status()).isEqualTo(UserStatus.ACTIVE);
      assertThat(result.account().email()).isEqualTo(EMAIL);
      assertThat(result.account().generationNo()).isNull();
    }

    @Test
    @DisplayName("재로그인이면 newUser 가 false 이고 계정을 새로 만들지 않는다")
    void doesNotDuplicate() {
      userAccountService.registerOrUpdateOAuthUser(registration("홍길동", "https://cdn/a.png"));
      long count = userRepository.count();

      OAuthRegistrationResult result =
          userAccountService.registerOrUpdateOAuthUser(registration("홍길동", "https://cdn/a.png"));

      assertThat(result.newUser()).isFalse();
      assertThat(userRepository.count()).isEqualTo(count);
    }

    @Test
    @DisplayName("재로그인 시 이름과 프로필 이미지를 제공자 값으로 갱신한다")
    void refreshesProfile() {
      userAccountService.registerOrUpdateOAuthUser(registration("홍길동", "https://cdn/a.png"));

      OAuthRegistrationResult result =
          userAccountService.registerOrUpdateOAuthUser(registration("홍길동2", "https://cdn/b.png"));

      assertThat(result.account().name()).isEqualTo("홍길동2");
      assertThat(result.account().profileImageUrl()).isEqualTo("https://cdn/b.png");
    }

    @Test
    @DisplayName("승격된 사용자가 재로그인해도 권한과 기수가 유지된다")
    void keepsRoleAndGeneration() {
      userAccountService.registerOrUpdateOAuthUser(registration("홍길동", null));
      User user = userRepository.findByProviderId(PROVIDER_ID).orElseThrow();
      user.promoteToMember(9);
      userRepository.flush();

      OAuthRegistrationResult result =
          userAccountService.registerOrUpdateOAuthUser(registration("홍길동", null));

      assertThat(result.account().role()).isEqualTo(Role.MEMBER);
      assertThat(result.account().generationNo()).isEqualTo(9);
    }
  }

  @Nested
  @DisplayName("findActiveById")
  class FindActiveById {

    @Test
    @DisplayName("지원서에서 수집한 값까지 담아 반환한다")
    void returnsFullAccount() {
      User user = userRepository.save(
          User.createGuest(PROVIDER_ID, EMAIL, "김부원", "https://cdn/a.png"));
      user.updateApplicantInfo("010-1234-5678", "경영대학", "경영학과", 3, "2021110000");
      user.promoteToMember(9);
      userRepository.flush();

      UserAccount account = userAccountService.findActiveById(user.getId()).orElseThrow();

      assertThat(account.phoneNumber()).isEqualTo("010-1234-5678");
      assertThat(account.college()).isEqualTo("경영대학");
      assertThat(account.major()).isEqualTo("경영학과");
      assertThat(account.studentYear()).isEqualTo(3);
      assertThat(account.studentNumber()).isEqualTo("2021110000");
      assertThat(account.generationNo()).isEqualTo(9);
    }

    @Test
    @DisplayName("탈퇴한 사용자는 반환하지 않는다")
    void excludesWithdrawnUser() {
      User user = userRepository.save(User.createGuest(PROVIDER_ID, EMAIL, "김부원", null));
      user.withdraw();
      userRepository.flush();

      assertThat(userAccountService.findActiveById(user.getId())).isEmpty();
    }

    @Test
    @DisplayName("없는 id 는 빈 Optional 이다")
    void returnsEmptyForUnknownId() {
      assertThat(userAccountService.findActiveById(999_999L)).isEmpty();
    }
  }
}
