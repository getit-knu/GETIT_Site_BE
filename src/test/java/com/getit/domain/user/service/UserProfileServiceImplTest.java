package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.TestStoredFiles;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.user.dto.OAuthUserRegistration;
import com.getit.domain.user.dto.ProfileEditCommand;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 본인 프로필 자기 수정. (이슈 #147) */
@SpringBootTest
@Transactional
class UserProfileServiceImplTest {

  private static final String PROVIDER_ID = "google-sub-profile";
  private static final String GOOGLE_NAME = "구글이름";
  private static final String GOOGLE_IMAGE = "https://lh3.googleusercontent.com/a.png";

  @Autowired
  private UserProfileService userProfileService;

  @Autowired
  private UserAccountService userAccountService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FileStorage fileStorage;

  private Long userId;

  @BeforeEach
  void signUpThroughOAuth() {
    userId = userAccountService.registerOrUpdateOAuthUser(
        new OAuthUserRegistration(PROVIDER_ID, "profile@getit.com", GOOGLE_NAME, GOOGLE_IMAGE))
        .account().id();
  }

  private void reLogin() {
    userAccountService.registerOrUpdateOAuthUser(
        new OAuthUserRegistration(PROVIDER_ID, "profile@getit.com", GOOGLE_NAME, GOOGLE_IMAGE));
  }

  private FileAsset publicImage(String key) {
    return TestStoredFiles.stored(fileAssetRepository, fileStorage,
        "public/" + key, key + ".png", "https://cdn/" + key, 1024L, "image/png", 1L);
  }

  private FileAsset privateFile() {
    return TestStoredFiles.stored(fileAssetRepository, fileStorage,
        "private/spec.pdf", "명세.pdf", "https://cdn/spec", 1024L, "application/pdf", 1L);
  }

  private User reload() {
    return userRepository.findById(userId).orElseThrow();
  }

  @Nested
  @DisplayName("editMyProfile")
  class EditMyProfile {

    @Test
    @DisplayName("이름과 전화번호를 저장한다")
    void savesNameAndPhoneNumber() {
      UserAccount result = userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", "010-1234-5678", null));

      assertThat(result.name()).isEqualTo("김겟잇");
      assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
      assertThat(reload().getName()).isEqualTo("김겟잇");
    }

    @Test
    @DisplayName("학과·학번·권한 같은 값은 자기 수정으로 바뀌지 않는다")
    void doesNotTouchFieldsOutsideSelfEdit() {
      User before = reload();

      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, null));

      User after = reload();
      assertThat(after.getMajor()).isEqualTo(before.getMajor());
      assertThat(after.getStudentNumber()).isEqualTo(before.getStudentNumber());
      assertThat(after.getRole()).isEqualTo(before.getRole());
      assertThat(after.getGenerationNo()).isEqualTo(before.getGenerationNo());
      assertThat(after.getStatus()).isEqualTo(before.getStatus());
    }

    @Test
    @DisplayName("파일 id 를 주면 사진 주소가 바뀌고 그 파일이 연결된다")
    void changesProfileImage() {
      FileAsset image = publicImage("me");

      UserAccount result = userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", null, image.getId()));

      assertThat(result.profileImageUrl()).isEqualTo("http://localhost:8080/api/public/files/public/me");
      assertThat(reload().getProfileFileId()).isEqualTo(image.getId());
      assertThat(fileAssetRepository.findById(image.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("사진을 다시 바꾸면 이전 파일은 연결이 풀린다")
    void disconnectsPreviousImage() {
      FileAsset first = publicImage("first");
      FileAsset second = publicImage("second");
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, first.getId()));

      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, second.getId()));

      assertThat(fileAssetRepository.findById(first.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.PENDING);
      assertThat(fileAssetRepository.findById(second.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
      assertThat(reload().getProfileFileId()).isEqualTo(second.getId());
    }

    @Test
    @DisplayName("파일 id 를 비워 보내면 사진은 그대로 남는다")
    void keepsImageWhenFileIdIsAbsent() {
      FileAsset image = publicImage("me");
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, image.getId()));

      userProfileService.editMyProfile(userId, new ProfileEditCommand("이름만바꿈", null, null));

      // 사진을 지우는 게 아니라 건드리지 않는 것이다. 지우면 구글 사진까지 사라진다.
      assertThat(reload().getProfileFileId()).isEqualTo(image.getId());
      assertThat(fileAssetRepository.findById(image.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("같은 파일 id 를 다시 보내도 연결을 건드리지 않는다")
    void keepsConnectionWhenSameFileIsSentAgain() {
      FileAsset image = publicImage("me");
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, image.getId()));

      // 다시 연결하려 들면 FILE_ALREADY_CONNECTED 로 막힌다.
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", "010-0000-0000", image.getId()));

      assertThat(fileAssetRepository.findById(image.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
      assertThat(reload().getPhoneNumber()).isEqualTo("010-0000-0000");
    }

    @Test
    @DisplayName("비공개 저장소 파일은 프로필 사진으로 쓸 수 없다")
    void rejectsPrivateFile() {
      FileAsset privateFile = privateFile();

      // 비공개 파일의 서명 주소는 몇 분 뒤 만료돼 프로필 사진이 깨진다.
      assertThatThrownBy(() -> userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", null, privateFile.getId())))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NOT_PUBLIC_PROFILE_IMAGE);
    }

    @Test
    @DisplayName("탈퇴한 사용자면 USER_NOT_FOUND")
    void throwsWhenUserWithdrawn() {
      reload().withdraw();

      assertThatThrownBy(() -> userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", null, null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("OAuth 재로그인과의 관계")
  class AgainstOAuthReLogin {

    @Test
    @DisplayName("스스로 고친 뒤에는 재로그인해도 이름과 사진이 구글 값으로 되돌아가지 않는다")
    void keepsSelfEditedProfileAcrossReLogin() {
      FileAsset image = publicImage("me");
      userProfileService.editMyProfile(
          userId, new ProfileEditCommand("내가정한이름", null, image.getId()));

      reLogin();

      // 재로그인마다 구글 값으로 덮어쓰면 자기 수정이 조용히 사라진다 (이슈 #147).
      User user = reload();
      assertThat(user.getName()).isEqualTo("내가정한이름");
      assertThat(user.getProfileImageUrl()).isNotEqualTo(GOOGLE_IMAGE);
    }

    @Test
    @DisplayName("고친 적이 없으면 재로그인 때 구글 값을 그대로 따라간다")
    void stillFollowsGoogleWhenNeverEdited() {
      userAccountService.registerOrUpdateOAuthUser(
          new OAuthUserRegistration(PROVIDER_ID, "profile@getit.com", "구글이바꾼이름", GOOGLE_IMAGE));

      assertThat(reload().getName()).isEqualTo("구글이바꾼이름");
    }
  }
}
