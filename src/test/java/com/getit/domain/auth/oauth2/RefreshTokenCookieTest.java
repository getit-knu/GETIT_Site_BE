package com.getit.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import com.getit.domain.auth.AuthProperties;

/**
 * Refresh 쿠키의 SameSite · Secure 가 설정값을 그대로 따르는지 본다.
 *
 * <p>다른 테스트는 기본 설정인 {@code Lax} 만 확인해서, 구현이 다시 하드코딩으로 돌아가도
 * 전부 통과한다. 교차 사이트 배포에서 {@code None} 이 나오지 않으면
 * 재발급 요청에 쿠키가 실리지 않아 "로그인은 되는데 새로고침하면 풀린다" 로 나타난다.
 * 원인을 찾기 어려운 증상이라 여기서 못을 박아둔다.
 */
class RefreshTokenCookieTest {

  private static RefreshTokenCookie with(String sameSite, boolean secure) {
    return new RefreshTokenCookie(
        new AuthProperties("http://localhost:5173/oauth/callback", secure, sameSite));
  }

  @Test
  @DisplayName("SameSite 설정이 발급 쿠키에 그대로 반영된다")
  void appliesConfiguredSameSiteToIssuedCookie() {
    ResponseCookie cookie = with("None", true).create("token", Duration.ofDays(14));

    assertThat(cookie.getSameSite()).isEqualTo("None");
    // None 은 Secure 가 함께여야 브라우저가 받는다.
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.isHttpOnly()).isTrue();
  }

  @Test
  @DisplayName("SameSite 설정이 삭제 쿠키에도 반영된다")
  void appliesConfiguredSameSiteToExpiredCookie() {
    ResponseCookie cookie = with("None", true).expired();

    // 발급과 속성이 하나라도 다르면 브라우저가 다른 쿠키로 보고 지우지 않는다.
    assertThat(cookie.getSameSite()).isEqualTo("None");
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getMaxAge()).isZero();
  }

  @Test
  @DisplayName("같은 사이트 배포에서는 설정한 Lax 가 그대로 나간다")
  void appliesLaxWhenConfigured() {
    assertThat(with("Lax", false).create("token", Duration.ofDays(14)).getSameSite())
        .isEqualTo("Lax");
  }
}
