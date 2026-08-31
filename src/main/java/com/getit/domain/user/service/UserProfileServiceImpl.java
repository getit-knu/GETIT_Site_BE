package com.getit.domain.user.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.user.dto.ProfileEditCommand;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
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

    if (imageChanged) {
      fileConnectionService.connectAll(List.of(newFileId));
      if (previousFileId != null) {
        fileConnectionService.disconnectAll(List.of(previousFileId));
      }
    }
    return UserAccount.from(user);
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
