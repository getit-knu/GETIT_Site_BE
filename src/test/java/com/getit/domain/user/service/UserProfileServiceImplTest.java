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
import com.getit.global.exception.CommonErrorCode;
import com.getit.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
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

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private MajorRepository majorRepository;

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

  /** 내가 올린 공개 이미지. */
  private FileAsset publicImage(String key) {
    return publicImageOf(key, userId);
  }

  private FileAsset publicImageOf(String key, Long uploaderId) {
    return TestStoredFiles.stored(fileAssetRepository, fileStorage,
        "public/" + key, key + ".png", "https://cdn/" + key, 1024L, "image/png", uploaderId);
  }

  private FileAsset privateFile() {
    return TestStoredFiles.stored(fileAssetRepository, fileStorage,
        "private/spec.pdf", "명세.pdf", "https://cdn/spec", 1024L, "application/pdf", userId);
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
          userId, new ProfileEditCommand("김겟잇", "010-1234-5678", null, null, null));

      assertThat(result.name()).isEqualTo("김겟잇");
      assertThat(result.phoneNumber()).isEqualTo("010-1234-5678");
      assertThat(reload().getName()).isEqualTo("김겟잇");
    }

    @Test
    @DisplayName("학과·학번·권한 같은 값은 자기 수정으로 바뀌지 않는다")
    void doesNotTouchFieldsOutsideSelfEdit() {
      User before = reload();

      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, null, null, null));

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
          userId, new ProfileEditCommand("김겟잇", null, image.getId(), null, null));

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
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, first.getId(), null, null));

      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, second.getId(), null, null));

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
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, image.getId(), null, null));

      userProfileService.editMyProfile(userId, new ProfileEditCommand("이름만바꿈", null, null, null, null));

      // 사진을 지우는 게 아니라 건드리지 않는 것이다. 지우면 구글 사진까지 사라진다.
      assertThat(reload().getProfileFileId()).isEqualTo(image.getId());
      assertThat(fileAssetRepository.findById(image.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("같은 파일 id 를 다시 보내도 연결을 건드리지 않는다")
    void keepsConnectionWhenSameFileIsSentAgain() {
      FileAsset image = publicImage("me");
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", null, image.getId(), null, null));

      // 다시 연결하려 들면 FILE_ALREADY_CONNECTED 로 막힌다.
      userProfileService.editMyProfile(userId, new ProfileEditCommand("김겟잇", "010-0000-0000", image.getId(), null, null));

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
          userId, new ProfileEditCommand("김겟잇", null, privateFile.getId(), null, null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.NOT_PUBLIC_PROFILE_IMAGE);
    }

    @Test
    @DisplayName("남이 올린 파일은 프로필 사진으로 쓸 수 없다")
    void rejectsSomeoneElsesFile() {
      FileAsset othersImage = publicImageOf("theirs", userId + 1000);

      // 막지 않으면 남이 올려둔 파일을 먼저 연결해 버릴 수 있고,
      // 정작 올린 사람은 FILE_ALREADY_CONNECTED 로 자기 파일을 못 쓴다.
      assertThatThrownBy(() -> userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", null, othersImage.getId(), null, null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
      assertThat(fileAssetRepository.findById(othersImage.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.PENDING);
    }

    @Test
    @DisplayName("탈퇴한 사용자면 USER_NOT_FOUND")
    void throwsWhenUserWithdrawn() {
      reload().withdraw();

      assertThatThrownBy(() -> userProfileService.editMyProfile(
          userId, new ProfileEditCommand("김겟잇", null, null, null, null)))
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
          userId, new ProfileEditCommand("내가정한이름", null, image.getId(), null, null));

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

  /**
   * 단과대 · 학과를 본인이 고친다. (이슈 #199)
   *
   * <p>이름이 아니라 id 로 받는다. 자유 입력으로 열면 "컴퓨터학부" 와 "컴퓨터공학부" 가
   * 섞여 나중에 손으로 정리해야 한다.
   */
  @Nested
  @DisplayName("단과대 · 학과 수정")
  class Affiliation {

    private College college;
    private Major major;

    @BeforeEach
    void setUpMasterData() {
      college = collegeRepository.save(College.create("IT대학"));
      major = majorRepository.save(Major.create(college.getId(), "컴퓨터학부"));
    }

    private ProfileEditCommand command(Long collegeId, Long majorId) {
      return new ProfileEditCommand("김겟잇", null, null, collegeId, majorId);
    }

    @Test
    @DisplayName("id 로 고르면 이름으로 저장된다")
    void savesNamesFromIds() {
      userProfileService.editMyProfile(userId, command(college.getId(), major.getId()));

      User saved = userRepository.findById(userId).orElseThrow();
      assertThat(saved.getCollege()).isEqualTo("IT대학");
      assertThat(saved.getMajor()).isEqualTo("컴퓨터학부");
    }

    @Test
    @DisplayName("보내지 않으면 그대로 둔다")
    void leavesUntouchedWhenAbsent() {
      userProfileService.editMyProfile(userId, command(college.getId(), major.getId()));

      userProfileService.editMyProfile(userId, command(null, null));

      User saved = userRepository.findById(userId).orElseThrow();
      assertThat(saved.getCollege()).isEqualTo("IT대학");
      assertThat(saved.getMajor()).isEqualTo("컴퓨터학부");
    }

    @Test
    @DisplayName("한쪽만 보내면 거부한다")
    void rejectsPartialAffiliation() {
      // 학과는 단과대에 속한다. 한쪽만 바꾸면 어긋난 조합이 남는다.
      assertThatThrownBy(() -> userProfileService.editMyProfile(userId, command(college.getId(), null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.AFFILIATION_INCOMPLETE);
      assertThatThrownBy(() -> userProfileService.editMyProfile(userId, command(null, major.getId())))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.AFFILIATION_INCOMPLETE);
    }

    @Test
    @DisplayName("다른 단과대의 학과는 거부한다")
    void rejectsMajorFromAnotherCollege() {
      College business = collegeRepository.save(College.create("경영대학"));

      // 막지 않으면 "IT대학 / 경영학과" 같은 조합이 저장된다.
      assertThatThrownBy(() ->
          userProfileService.editMyProfile(userId, command(business.getId(), major.getId())))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.MAJOR_NOT_IN_COLLEGE);
    }

    @Test
    @DisplayName("없는 id 는 거부한다")
    void rejectsUnknownIds() {
      assertThatThrownBy(() -> userProfileService.editMyProfile(userId, command(999_999L, major.getId())))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.COLLEGE_NOT_FOUND);
      assertThatThrownBy(() -> userProfileService.editMyProfile(userId, command(college.getId(), 999_999L)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.MAJOR_NOT_FOUND);
    }
  }
}
