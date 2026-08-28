package com.getit.domain.auth.oauth2;

import java.util.Base64;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import com.getit.domain.auth.AuthProperties;

/**
 * OAuth2 인증 요청을 세션이 아니라 쿠키에 보관한다.
 *
 * <p>기본 구현({@code HttpSessionOAuth2AuthorizationRequestRepository})은 HttpSession 을 쓴다.
 * 이 애플리케이션은 {@code SessionCreationPolicy.STATELESS} 라 세션에 기대면 안 되고,
 * 실제로 구글에서 돌아올 때 세션을 찾지 못해 {@code authorization_request_not_found} 로 로그인이 실패했다.
 *
 * <p>쿠키에 담으면 서버가 상태를 들고 있지 않아도 되고, 인스턴스를 여러 대로 늘려도
 * 세션 공유(sticky session · Redis)를 신경 쓸 필요가 없다.
 *
 * <p>SameSite 는 Lax 다. 구글에서 돌아오는 것은 top-level 이동이라 Lax 로 전송된다.
 * 프론트가 iframe 이나 fetch 로 로그인을 시도하면 전송되지 않으므로,
 * <b>로그인은 반드시 top-level 이동(window.location)으로 시작해야 한다.</b>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CookieOAuth2AuthorizationRequestRepository
    implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

  private static final String COOKIE_NAME = "oauth2_auth_request";

  /** 구글을 다녀오는 데 걸리는 시간만 살아 있으면 된다. */
  private static final int MAX_AGE_SECONDS = 300;

  private final AuthProperties authProperties;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return readCookie(request)
        .map(Cookie::getValue)
        .map(this::deserialize)
        .orElse(null);
  }

  @Override
  public void saveAuthorizationRequest(
      OAuth2AuthorizationRequest authorizationRequest,
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    // 인증 요청이 null 이면 삭제 요청이다 (기본 구현의 계약).
    if (authorizationRequest == null) {
      expireCookie(response);
      return;
    }
    write(response, serialize(authorizationRequest), MAX_AGE_SECONDS);
  }

  @Override
  public OAuth2AuthorizationRequest removeAuthorizationRequest(
      HttpServletRequest request,
      HttpServletResponse response
  ) {
    OAuth2AuthorizationRequest loaded = loadAuthorizationRequest(request);
    expireCookie(response);
    return loaded;
  }

  private java.util.Optional<Cookie> readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return java.util.Optional.empty();
    }
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName())) {
        return java.util.Optional.of(cookie);
      }
    }
    return java.util.Optional.empty();
  }

  private void write(HttpServletResponse response, String value, int maxAge) {
    ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
        .httpOnly(true)
        .secure(authProperties.refreshCookieSecure())
        // 구글 → 우리 서버 콜백은 top-level 이동이라 Lax 로 전송된다.
        .sameSite("Lax")
        .path("/")
        .maxAge(maxAge)
        .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  private void expireCookie(HttpServletResponse response) {
    write(response, "", 0);
  }

  private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
    return Base64.getUrlEncoder()
        .encodeToString(SerializationUtils.serialize(authorizationRequest));
  }

  private OAuth2AuthorizationRequest deserialize(String value) {
    try {
      Object deserialized = SerializationUtils.deserialize(Base64.getUrlDecoder().decode(value));
      return deserialized instanceof OAuth2AuthorizationRequest request ? request : null;
    } catch (IllegalArgumentException | org.springframework.core.serializer.support.SerializationFailedException e) {
      // 값이 깨졌거나 이전 버전 형식이다. 로그인만 다시 하면 되므로 실패로 만들지 않는다.
      log.warn("OAuth2 인증 요청 쿠키를 읽지 못했습니다. 로그인을 다시 시도해야 합니다.");
      return null;
    }
  }
}
