package com.getit.domain.auth.oauth2;

import com.getit.domain.auth.AuthProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 을 담는 쿠키를 만든다.
 *
 * <p>HttpOnly 라 자바스크립트가 읽을 수 없다. localStorage 에 두면 XSS 한 번에 탈취된다.
 *
 * <p>SameSite 는 설정으로 뺐다. 프론트와 백엔드가 같은 등록 도메인이면 Lax 가 안전하지만,
 * 서로 다른 사이트면 Lax 로는 쿠키가 전송되지 않아 토큰 재발급이 실패한다.
 * 도메인 구성이 바뀔 때 코드를 다시 배포하지 않으려고 환경변수로 바꿀 수 있게 했다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

  public static final String NAME = "refresh_token";

  private final AuthProperties authProperties;

  public ResponseCookie create(String token, Duration validity) {
    return base(token)
        .maxAge(validity)
        .build();
  }

  /** 로그아웃 시 즉시 만료시킨다. */
  public ResponseCookie expired() {
    return base("")
        .maxAge(0)
        .build();
  }

  private ResponseCookie.ResponseCookieBuilder base(String value) {
    return ResponseCookie.from(NAME, value)
        .httpOnly(true)
        .secure(authProperties.refreshCookieSecure())
        .sameSite(authProperties.refreshCookieSameSite())
        .path("/");
  }
}
