package com.getit.domain.user.service;

import com.getit.domain.user.dto.ProfileEditCommand;
import com.getit.domain.user.dto.UserAccount;

/**
 * 본인 프로필 자기 수정 계약. (이슈 #147)
 *
 * <p>auth 가 {@code PUT /api/auth/me} 에서 쓴다. 조회({@code GET /api/auth/me})와 짝이다.
 *
 * @see UserAccountService 도메인 경계를 넘는 계약을 제공자 패키지에 두는 이유
 */
public interface UserProfileService {

  /** 권한과 무관하게 자기 것만 고친다. GUEST 도 호출할 수 있다. */
  UserAccount editMyProfile(Long userId, ProfileEditCommand command);
}
