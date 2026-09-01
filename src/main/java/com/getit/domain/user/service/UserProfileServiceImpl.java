package com.getit.domain.user.service;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.user.dto.ProfileEditCommand;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserProfileServiceImpl implements UserProfileService {

  private final UserRepository userRepository;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;
  private final CollegeRepository collegeRepository;
  private final MajorRepository majorRepository;

  /**
   * 사진은 파일 id 로 받는다. 주소를 그대로 받으면 아무 주소나 넣을 수 있고, 우리가 올린
   * 파일인지도 알 수 없다.
   *
   * <p>받은 파일 id 는 두 가지를 확인한다. <b>본인이 올린 것</b>이어야 하고 — 아니면 남이
   * 올려둔 파일 id 를 넘겨 먼저 연결해 버릴 수 있고, 정작 올린 사람은
   * {@code FILE_ALREADY_CONNECTED} 로 자기 파일을 못 쓰게 된다(PR #164 리뷰 지적) —
   * <b>공개 컨테이너</b>에 있어야 한다. 비공개 파일의 서명 주소는 몇 분 뒤 만료돼
   * 프로필 사진이 깨진다.
   *
   * <p>사용자 행을 잠그고 시작한다. 사진 교체는 이전 파일을 읽어 연결을 푸는
   * read-modify-write 라, 같은 사용자의 동시 요청이 겹치면 진 쪽이 연결한 파일이
   * 아무도 가리키지 않는 채로 남는다.
   */
  @Override
  public UserAccount editMyProfile(Long userId, ProfileEditCommand command) {
    User user = userRepository.findByIdForUpdate(userId)
        .filter(found -> !found.isDeleted())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    Long previousFileId = user.getProfileFileId();
    Long newFileId = command.profileFileId();
    boolean imageChanged = newFileId != null && !newFileId.equals(previousFileId);

    String newImageUrl = imageChanged ? validProfileImageUrl(newFileId, userId) : null;
    user.editProfile(command.name(), command.phoneNumber(), newImageUrl,
        imageChanged ? newFileId : null);

    if (command.changesAffiliation()) {
      applyAffiliation(user, command.collegeId(), command.majorId());
    }

    if (imageChanged) {
      fileConnectionService.connectAll(List.of(newFileId));
      if (previousFileId != null) {
        fileConnectionService.disconnectAll(List.of(previousFileId));
      }
    }
    return UserAccount.from(user);
  }

  /**
   * 단과대 · 학과를 id 로 받아 이름으로 바꿔 넣는다. (이슈 #199)
   *
   * <p>{@code User} 는 소속을 이름으로 들고 있다. 승격(9.4)이 지원서의 id 를 이름으로 바꿔
   * 복사하는 것과 같은 방식이다.
   *
   * <p>이름이 아니라 id 로 받는 이유는 값을 통일하기 위해서다. 자유 입력으로 열면
   * "컴퓨터학부" 와 "컴퓨터공학부" 가 섞여 나중에 손으로 정리해야 한다.
   *
   * <p>둘은 함께 와야 한다. 학과는 단과대에 속하므로 한쪽만 바꾸는 것은 뜻이 없고,
   * 그대로 두면 "IT대학 / 경영학과" 같은 조합이 저장된다.
   */
  private void applyAffiliation(User user, Long collegeId, Long majorId) {
    if (collegeId == null || majorId == null) {
      throw new BusinessException(UserErrorCode.AFFILIATION_INCOMPLETE);
    }

    College college = collegeRepository.findById(collegeId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.COLLEGE_NOT_FOUND));
    Major major = majorRepository.findById(majorId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.MAJOR_NOT_FOUND));

    if (!major.getCollegeId().equals(college.getId())) {
      throw new BusinessException(UserErrorCode.MAJOR_NOT_IN_COLLEGE);
    }

    user.updateAffiliation(college.getName(), major.getName());
  }

  private String validProfileImageUrl(Long fileId, Long userId) {
    FileInfo file = fileQueryService.findById(fileId);
    if (!Objects.equals(file.uploaderId(), userId)) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }
    if (!file.publiclyReadable()) {
      throw new BusinessException(UserErrorCode.NOT_PUBLIC_PROFILE_IMAGE);
    }
    return file.url();
  }
}
