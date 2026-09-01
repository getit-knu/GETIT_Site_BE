package com.getit.domain.auth.dto;

import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

/**
 * 내 프로필 응답. (API 명세서 1.5)
 *
 * <p>필드명과 순서를 명세서 JSON 그대로 맞췄다. 프론트가 앱 진입 시 이 응답으로 권한을 판단한다.
 *
 * <p>명세서 1.5 에는 roleLabel · statusLabel 이 없어 넣지 않았다. 목록 API(9.1)와 다른 점이다.
 * 화면에 권한을 표기해야 한다면 추가할 수 있다.
 */
public record MeResponse(
    Long id,
    String email,
    String name,
    String phoneNumber,
    String college,
    String major,
    Integer studentYear,
    String studentNumber,
    String profileImageUrl,
    Role role,
    Integer generationNo,
    UserStatus status,

    /**
     * 개인정보 수집·이용에 동의한 시각. 아직 동의하지 않았으면 null. (이슈 #203)
     *
     * <p>프론트가 재로그인 시 "이미 동의했는지"를 판단하는 근거다. 신규 가입자 온보딩에서
     * {@code POST /api/auth/consent} 로 채워진다.
     */
    LocalDateTime privacyConsentedAt
) {

  public static MeResponse from(UserAccount account) {
    return new MeResponse(
        account.id(),
        account.email(),
        account.name(),
        account.phoneNumber(),
        account.college(),
        account.major(),
        account.studentYear(),
        account.studentNumber(),
        account.profileImageUrl(),
        account.role(),
        account.generationNo(),
        account.status(),
        account.privacyConsentedAt()
    );
  }
}
