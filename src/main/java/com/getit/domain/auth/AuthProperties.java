package com.getit.domain.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증 흐름 설정.
 *
 * @param oauth2RedirectUri 로그인 성공 후 사용자를 돌려보낼 프론트 주소
 * @param refreshCookieSecure Refresh Token 쿠키의 Secure 플래그.
 *                            로컬은 http 라 false, 배포 환경은 반드시 true 여야 한다
 * @param refreshCookieSameSite Refresh Token 쿠키의 SameSite 정책.
 *                              <p>프론트와 백엔드가 같은 등록 도메인이면 {@code Lax} 를 쓴다
 *                              (예: getit.co.kr ↔ api.getit.co.kr). 가장 안전하다.
 *                              <p>서로 다른 사이트면 {@code Lax} 로는 쿠키가 전송되지 않아
 *                              토큰 재발급이 실패한다. 그때만 {@code None} 을 쓴다.
 *                              {@code None} 은 Secure 가 함께 true 여야 브라우저가 받는다.
 */
@ConfigurationProperties(prefix = "getit.auth")
public record AuthProperties(
    String oauth2RedirectUri,
    boolean refreshCookieSecure,
    String refreshCookieSameSite
) { }
