package com.getit.domain.user.service;

import com.getit.domain.user.dto.OAuthRegistrationResult;
import com.getit.domain.user.dto.OAuthUserRegistration;
import com.getit.domain.user.dto.UserAccount;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * auth 가 소비하는 사용자 계약. (작업 분할 계획 4.2)
 *
 * <p>다른 도메인은 UserRepository 를 직접 참조하지 않는다. 인터페이스는 제공자 패키지에 두고
 * 소비자가 주입받는다. 그래야 user 의 내부 구조가 바뀌어도 auth 가 함께 깨지지 않는다.
 *
 * <p>Coding Convention 은 Service 에 인터페이스를 두지 않도록 하고 있으나,
 * 그것은 도메인 내부 서비스에 대한 규칙이다. 도메인 경계를 넘는 계약은 4.2 를 따른다.
 *
 * <p>대시보드 · 제출 현황이 쓰는 조회 계약({@code findActiveMembers} · {@code countMembers})은
 * 소비자가 B 라 성격이 다르므로 별도 인터페이스로 A 가 추가한다.
 */
public interface UserAccountService {

  /**
   * OAuth 로그인 사용자를 등록하거나 갱신한다. (명세서 1.2)
   *
   * <p>providerId 로 기존 계정을 찾는다. 없으면 GUEST 로 만들고, 있으면 이름과 프로필 이미지를
   * 제공자 쪽 값으로 갱신한다. email 과 providerId 는 식별자라 갱신하지 않는다.
   */
  OAuthRegistrationResult registerOrUpdateOAuthUser(OAuthUserRegistration registration);

  /**
   * 활동 중인 사용자를 조회한다.
   *
   * <p>탈퇴(soft delete)한 사용자는 반환하지 않는다. 토큰은 유효한데 계정이 사라진 경우를
   * 소비자가 매번 걸러내지 않도록 여기서 처리한다.
   */
  Optional<UserAccount> findActiveById(Long userId);

  /**
   * 개인정보 수집·이용 동의를 기록한다. (이슈 #203)
   *
   * <p>이미 동의한 사용자는 최초 동의 시각을 그대로 둔다 — 재호출로 시각이 밀리면 입증에 쓸
   * 값이 최신 요청 시각으로 덮인다. 그래서 호출부가 중복 호출을 걸러낼 필요가 없다(멱등).
   *
   * @param consentedAt 동의 시각. 호출부가 {@code Clock} 으로 만들어 넘긴다
   * @return 기록 이후의 사용자 정보
   * @throws com.getit.global.exception.BusinessException 활동 중인 사용자가 없으면
   */
  UserAccount recordPrivacyConsent(Long userId, LocalDateTime consentedAt);
}
