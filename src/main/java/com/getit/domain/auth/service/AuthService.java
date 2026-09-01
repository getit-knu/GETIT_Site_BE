package com.getit.domain.auth.service;

import com.getit.domain.auth.dto.MeResponse;
import com.getit.domain.auth.dto.MeUpdateRequest;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.service.UserAccountService;
import com.getit.domain.user.service.UserProfileService;
import com.getit.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 사용자 조회.
 *
 * <p>Coding Convention 에 따라 인터페이스를 두지 않고 구현을 클래스 안에 그대로 작성한다.
 * 사용자 데이터는 UserRepository 가 아니라 user 패키지가 제공하는 계약을 통해 읽는다
 * (작업 분할 계획 4.2).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

  private final UserAccountService userAccountService;
  private final UserProfileService userProfileService;

  /**
   * 동의 시각을 만드는 시각 소스. (이슈 #203)
   *
   * <p>{@code LocalDateTime.now()} 를 직접 부르면 "이미 동의한 사용자는 최초 시각을 유지한다"는
   * 규칙을 테스트에서 시각으로 확인할 수 없다.
   */
  private final Clock clock;

  /**
   * 내 프로필 조회. (1.5 GET /api/auth/me)
   *
   * <p>토큰은 유효한데 사용자가 없는 경우가 있다. 탈퇴 처리된 뒤 만료 전 토큰으로 접근하는 상황이다.
   */
  public MeResponse getMe(Long userId) {
    return userAccountService.findActiveById(userId)
        .map(MeResponse::from)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }

  /**
   * 내 프로필 수정. (이슈 #147)
   *
   * <p>GUEST · MEMBER · ADMIN 모두 호출한다. 자기 것만 고치므로 권한으로 나누지 않는다.
   */
  @Transactional
  public MeResponse updateMe(Long userId, MeUpdateRequest request) {
    return MeResponse.from(userProfileService.editMyProfile(userId, request.toCommand()));
  }

  /**
   * 개인정보 수집·이용 동의 기록. (이슈 #203)
   *
   * <p>구글 OAuth 는 프론트가 {@code <a href>} 로 바로 넘어가는 구조라 서버가 진입을 막을
   * 여지가 없다. 게다가 로그인 앞에 동의 게이트를 두면 기존 회원이 로그인할 때마다 다시
   * 동의를 눌러야 한다. 그래서 로그인 자체는 막지 않고, 프론트가 콜백의 {@code isNewUser}
   * 로 신규 가입자를 가려내 온보딩에서 이 엔드포인트를 호출한다.
   *
   * <p>이미 동의한 사용자가 다시 호출해도 성공한다. 최초 동의 시각은 그대로 유지된다.
   */
  @Transactional
  public MeResponse consentToPrivacy(Long userId) {
    return MeResponse.from(
        userAccountService.recordPrivacyConsent(userId, LocalDateTime.now(clock)));
  }
}
