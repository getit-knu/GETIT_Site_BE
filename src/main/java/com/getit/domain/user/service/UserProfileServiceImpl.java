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
import java.util.List;
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
   * 파일인지도 알 수 없다. 공개 컨테이너의 파일만 받는다 — 비공개 파일의 서명 주소는
   * 몇 분 뒤 만료돼 프로필 사진이 깨진다.
   *
   * <p>엔티티 변경을 파일 연결보다 먼저 한다. 연결 쿼리가 영속성 컨텍스트를 비우기 때문에
   * 그 뒤에 손대면 반영되지 않는다 (PR #82 · #152 에서 두 번 걸렸다).
   */
  @Override
  public UserAccount editMyProfile(Long userId, ProfileEditCommand command) {
    User user = userRepository.findById(userId)
        .filter(found -> !found.isDeleted())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    Long previousFileId = user.getProfileFileId();
    Long newFileId = command.profileFileId();
    boolean imageChanged = newFileId != null && !newFileId.equals(previousFileId);

    String newImageUrl = imageChanged ? validPublicImageUrl(newFileId) : null;
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

  private String validPublicImageUrl(Long fileId) {
    FileInfo file = fileQueryService.findById(fileId);
    if (!file.publiclyReadable()) {
      throw new BusinessException(UserErrorCode.NOT_PUBLIC_PROFILE_IMAGE);
    }
    return file.url();
  }
}
