package com.getit.domain.auth.oauth2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.AuthProperties;
import com.getit.domain.auth.jwt.JwtProperties;

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
 * <h2>쿠키를 신뢰하지 않는다</h2>
 *
 * <p>쿠키 값은 클라이언트가 마음대로 바꿔 보낼 수 있다. 그래서 두 가지를 지킨다.
 *
 * <ul>
 *   <li><b>Java 네이티브 역직렬화를 쓰지 않는다.</b> 임의의 바이트를 역직렬화하면
 *       클래스패스의 gadget 을 타고 타입 검사 전에 코드가 실행될 수 있다.
 *       필요한 필드만 JSON 으로 담고 고정된 타입으로 되돌린다.
 *   <li><b>HMAC 을 먼저 검증한다.</b> 서명이 맞지 않으면 JSON 을 파싱조차 하지 않는다.
 *       공격자가 state 를 심어 피해자를 자기 계정으로 로그인시키는 로그인 CSRF 도 함께 막힌다.
 * </ul>
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

  private static final String HMAC_ALGORITHM = "HmacSHA256";

  /** 브라우저가 쿠키 하나에 허용하는 크기. 넘으면 조용히 버려서 원인 찾기가 어렵다. */
  private static final int BROWSER_COOKIE_LIMIT = 4096;

  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private final AuthProperties authProperties;
  private final JwtProperties jwtProperties;
  private final ObjectMapper objectMapper;

  @Override
  public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
    return readCookie(request)
        .map(Cookie::getValue)
        .map(this::decode)
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
    write(response, encode(authorizationRequest), MAX_AGE_SECONDS);
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

  // ---------------------------------------------------------------------------
  // 쿠키 입출력
  // ---------------------------------------------------------------------------

  private Optional<Cookie> readCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    for (Cookie cookie : cookies) {
      if (COOKIE_NAME.equals(cookie.getName())) {
        return Optional.of(cookie);
      }
    }
    return Optional.empty();
  }

  private void write(HttpServletResponse response, String value, int maxAge) {
    if (value.length() > BROWSER_COOKIE_LIMIT) {
      // 브라우저가 이 쿠키를 통째로 버리면 콜백에서 authorization_request_not_found 가 난다.
      // 조용히 실패하면 원인을 못 찾으므로 여기서 남긴다.
      log.error(
          "OAuth2 인증 요청 쿠키가 {}바이트로 브라우저 한계({})를 넘었습니다. 로그인이 실패합니다.",
          value.length(), BROWSER_COOKIE_LIMIT);
    }
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

  // ---------------------------------------------------------------------------
  // 인코딩 — {payload}.{서명}
  // ---------------------------------------------------------------------------

  private String encode(OAuth2AuthorizationRequest authorizationRequest) {
    try {
      String payload = ENCODER.encodeToString(
          objectMapper.writeValueAsBytes(Payload.from(authorizationRequest)));
      return payload + "." + sign(payload);
    } catch (Exception e) {
      // 여기서 실패하면 로그인 자체를 시작할 수 없다. 삼키면 안 된다.
      throw new IllegalStateException("OAuth2 인증 요청을 쿠키로 만들지 못했습니다.", e);
    }
  }

  /**
   * 서명을 먼저 확인하고, 통과한 값만 JSON 으로 되돌린다.
   *
   * <p>값이 깨졌거나 위조됐거나 이전 버전 형식이면 {@code null} 을 준다.
   * 로그인을 다시 하면 되는 일이라 예외로 만들지 않는다.
   */
  private OAuth2AuthorizationRequest decode(String value) {
    try {
      int separator = value.lastIndexOf('.');
      if (separator < 0) {
        return rejected("형식이 올바르지 않습니다");
      }

      String payload = value.substring(0, separator);
      String signature = value.substring(separator + 1);

      // 타이밍 공격을 피하려고 상수 시간 비교를 쓴다.
      if (!MessageDigest.isEqual(
          sign(payload).getBytes(StandardCharsets.UTF_8),
          signature.getBytes(StandardCharsets.UTF_8))) {
        return rejected("서명이 일치하지 않습니다");
      }

      return objectMapper.readValue(DECODER.decode(payload), Payload.class).toRequest();
    } catch (Exception e) {
      // Base64 · JSON · 필수 필드 누락을 한데 모아 처리한다.
      // 여기서 예외가 새어 나가면 깨진 쿠키 하나로 500 이 난다.
      return rejected(e.getClass().getSimpleName());
    }
  }

  private OAuth2AuthorizationRequest rejected(String reason) {
    log.warn("OAuth2 인증 요청 쿠키를 받아들이지 않았습니다 ({}). 로그인을 다시 시도해야 합니다.", reason);
    return null;
  }

  private String sign(String payload) throws Exception {
    Mac mac = Mac.getInstance(HMAC_ALGORITHM);
    mac.init(new SecretKeySpec(
        jwtProperties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
    return ENCODER.encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
  }

  // ---------------------------------------------------------------------------
  // 쿠키에 담는 필드 — 여기 적힌 것만 오간다
  // ---------------------------------------------------------------------------

  /**
   * {@link OAuth2AuthorizationRequest} 중 콜백에서 복원에 필요한 필드만 추린 형태.
   *
   * <p>{@code attributes} 에는 Spring Security 가 콜백에서 어느 등록 정보를 쓸지 찾을 때 보는
   * {@code registration_id} 가 들어 있다. 빠지면 콜백이 처리되지 않는다.
   *
   * <p>{@code authorizationRequestUri} 는 담지 않는다. 구글로 내보낼 때 이미 쓰였고,
   * 나머지 필드로 다시 만들어진다.
   */
  private record Payload(
      String authorizationUri,
      String clientId,
      String redirectUri,
      Set<String> scopes,
      String state,
      Map<String, Object> additionalParameters,
      Map<String, Object> attributes
  ) {

    static Payload from(OAuth2AuthorizationRequest request) {
      return new Payload(
          request.getAuthorizationUri(),
          request.getClientId(),
          request.getRedirectUri(),
          request.getScopes(),
          request.getState(),
          request.getAdditionalParameters(),
          request.getAttributes());
    }

    OAuth2AuthorizationRequest toRequest() {
      return OAuth2AuthorizationRequest.authorizationCode()
          .authorizationUri(authorizationUri)
          .clientId(clientId)
          .redirectUri(redirectUri)
          .scopes(scopes == null ? new LinkedHashSet<>() : new LinkedHashSet<>(scopes))
          .state(state)
          .additionalParameters(
              additionalParameters == null ? new LinkedHashMap<>() : additionalParameters)
          .attributes(attributes == null ? new LinkedHashMap<>() : attributes)
          .build();
    }
  }
}
