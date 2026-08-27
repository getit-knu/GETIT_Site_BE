package com.getit.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserTest {

  private static final String PROVIDER_ID = "google-sub-1234567890";
  private static final String EMAIL = "member@getit.com";
  private static final String NAME = "김부원";
  private static final String IMAGE_URL = "https://lh3.googleusercontent.com/abc";

  private User createGuest() {
    return User.createGuest(PROVIDER_ID, EMAIL, NAME, IMAGE_URL);
  }

  @Nested
  @DisplayName("createGuest")
  class CreateGuest {

    @Test
    @DisplayName("최초 로그인 사용자는 GUEST · ACTIVE 로 생성된다")
    void createsGuestWithActiveStatus() {
      User user = createGuest();

      assertThat(user.getRole()).isEqualTo(Role.GUEST);
      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.getProviderId()).isEqualTo(PROVIDER_ID);
      assertThat(user.getEmail()).isEqualTo(EMAIL);
      assertThat(user.getName()).isEqualTo(NAME);
      assertThat(user.getProfileImageUrl()).isEqualTo(IMAGE_URL);
    }

    @Test
    @DisplayName("지원서에서 수집하는 값과 기수는 비어 있다")
    void leavesApplicationFieldsEmpty() {
      User user = createGuest();

      assertThat(user.getPhoneNumber()).isNull();
      assertThat(user.getCollege()).isNull();
      assertThat(user.getMajor()).isNull();
      assertThat(user.getStudentYear()).isNull();
      assertThat(user.getStudentNumber()).isNull();
      assertThat(user.getGenerationNo()).isNull();
    }

    @Test
    @DisplayName("생성 직후에는 삭제 상태가 아니다")
    void isNotDeleted() {
      assertThat(createGuest().isDeleted()).isFalse();
    }
  }

  @Nested
  @DisplayName("updateProfile")
  class UpdateProfile {

    @Test
    @DisplayName("이름과 프로필 이미지를 갱신한다")
    void updatesNameAndImage() {
      User user = createGuest();

      user.updateProfile("김운영", "https://cdn.getit.com/new.png");

      assertThat(user.getName()).isEqualTo("김운영");
      assertThat(user.getProfileImageUrl()).isEqualTo("https://cdn.getit.com/new.png");
    }

    @Test
    @DisplayName("식별자인 email 과 providerId 는 갱신하지 않는다")
    void keepsIdentifiers() {
      User user = createGuest();

      user.updateProfile("김운영", "https://cdn.getit.com/new.png");

      assertThat(user.getEmail()).isEqualTo(EMAIL);
      assertThat(user.getProviderId()).isEqualTo(PROVIDER_ID);
    }
  }

  @Nested
  @DisplayName("promoteToMember")
  class PromoteToMember {

    @Test
    @DisplayName("GUEST 를 MEMBER 로 승격하고 소속 기수를 부여한다")
    void promotesGuestWithGeneration() {
      User user = createGuest();

      user.promoteToMember(9);

      assertThat(user.getRole()).isEqualTo(Role.MEMBER);
      assertThat(user.getGenerationNo()).isEqualTo(9);
    }
  }

  @Nested
  @DisplayName("withdraw · activate")
  class WithdrawAndActivate {

    @Test
    @DisplayName("탈퇴하면 상태가 WITHDRAWN 이 되고 soft delete 된다")
    void withdrawMarksStatusAndDeletedAt() {
      User user = createGuest();

      user.withdraw();

      assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
      assertThat(user.isDeleted()).isTrue();
      assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("복구하면 ACTIVE 로 돌아오고 deletedAt 이 해제된다")
    void activateRestoresUser() {
      User user = createGuest();
      user.withdraw();

      user.activate();

      assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
      assertThat(user.isDeleted()).isFalse();
      assertThat(user.getDeletedAt()).isNull();
    }
  }

  @Nested
  @DisplayName("Role")
  class RoleEnum {

    @Test
    @DisplayName("화면 표기용 한글 라벨을 갖는다")
    void hasKoreanLabel() {
      assertThat(Role.GUEST.getLabel()).isEqualTo("비회원");
      assertThat(Role.MEMBER.getLabel()).isEqualTo("부원");
      assertThat(Role.ADMIN.getLabel()).isEqualTo("운영진");
    }

    @Test
    @DisplayName("Spring Security 용 권한 문자열은 ROLE_ 접두사를 갖는다")
    void hasRolePrefixedAuthority() {
      assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }
  }
}
