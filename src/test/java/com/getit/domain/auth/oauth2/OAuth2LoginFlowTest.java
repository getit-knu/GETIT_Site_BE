package com.getit.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OAuth2 로그인 진입점. (명세서 1.1)
 *
 * <p>Google 로 나가는 리다이렉트까지만 확인한다. 그 뒤는 외부 서비스라 통합 테스트로 검증할 수 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OAuth2LoginFlowTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

  @Test
  @DisplayName("인증 없이 Google 로그인 진입점에 접근할 수 있다")
  void authorizationEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/oauth2/authorization/google"))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  @DisplayName("Google 동의 화면으로 리다이렉트하며 요청한 scope 를 싣는다")
  void redirectsToGoogleWithScopes() throws Exception {
    MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")).andReturn();

    String location = result.getResponse().getRedirectedUrl();
    assertThat(location)
        .startsWith("https://accounts.google.com/o/oauth2/v2/auth")
        .contains("scope=profile%20email")
        .contains("response_type=code")
        .contains("state=");
  }

  @Test
  @DisplayName("authorization request 를 세션이 아니라 쿠키에 담는다")
  void storesAuthorizationRequestInCookie() throws Exception {
    MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")).andReturn();

    // 세션에 담으면 STATELESS 환경에서 콜백 때 찾지 못해
    // authorization_request_not_found 로 로그인이 실패한다. 실제로 겪었던 문제다.
    assertThat(result.getResponse().getCookie("oauth2_auth_request"))
        .as("인증 요청 쿠키가 없으면 콜백에서 요청을 복원할 수 없다")
        .isNotNull()
        .satisfies(cookie -> {
          assertThat(cookie.getValue()).isNotBlank();
          assertThat(cookie.isHttpOnly()).isTrue();
        });
  }

  @Test
  @DisplayName("인증 요청 쿠키는 구글을 다녀올 동안만 살아 있다")
  void authorizationRequestCookieIsShortLived() throws Exception {
    MvcResult result = mockMvc.perform(get("/oauth2/authorization/google")).andReturn();

    assertThat(result.getResponse().getCookie("oauth2_auth_request").getMaxAge())
        .isPositive()
        .isLessThanOrEqualTo(600);
  }

  @Test
  @DisplayName("진입점에서 받은 쿠키를 콜백 요청에 실으면 인증 요청이 그대로 복원된다")
  void restoresAuthorizationRequestFromCookieOnCallback() throws Exception {
    // 이번 장애의 핵심은 저장이 아니라 '다음 요청에서의 복원' 이었다.
    // 응답 쿠키를 새 요청에 실어 실제로 왕복시킨다.
    Cookie issued = authorize().getResponse().getCookie("oauth2_auth_request");

    MockHttpServletRequest callback = new MockHttpServletRequest();
    callback.setCookies(issued);

    OAuth2AuthorizationRequest restored =
        authorizationRequestRepository.loadAuthorizationRequest(callback);

    assertThat(restored)
        .as("복원되지 않으면 콜백이 authorization_request_not_found 로 실패한다")
        .isNotNull();
    assertThat(restored.getState()).isEqualTo(stateOf(issued));
    assertThat(restored.getAuthorizationUri())
        .isEqualTo("https://accounts.google.com/o/oauth2/v2/auth");
    assertThat(restored.getScopes()).containsExactlyInAnyOrder("profile", "email");
    // 이게 없으면 Spring 이 콜백에서 어느 등록 정보를 쓸지 몰라 처리 자체를 못 한다.
    assertThat(restored.getAttributes()).containsKey(OAuth2ParameterNames.REGISTRATION_ID);
  }

  @Test
  @DisplayName("복원한 인증 요청을 제거하면 쿠키가 만료된다")
  void removingAuthorizationRequestExpiresCookie() throws Exception {
    Cookie issued = authorize().getResponse().getCookie("oauth2_auth_request");

    MockHttpServletRequest callback = new MockHttpServletRequest();
    callback.setCookies(issued);
    MockHttpServletResponse response = new MockHttpServletResponse();

    assertThat(authorizationRequestRepository.removeAuthorizationRequest(callback, response))
        .as("제거할 때도 원래 요청을 돌려줘야 한다")
        .isNotNull();
    assertThat(response.getCookie("oauth2_auth_request").getMaxAge())
        .as("쿠키가 남아 있으면 다음 로그인에서 지난 요청이 섞인다")
        .isZero();
  }

  @Test
  @DisplayName("쿠키를 위조하면 복원하지 않는다")
  void rejectsTamperedCookie() throws Exception {
    Cookie issued = authorize().getResponse().getCookie("oauth2_auth_request");

    // 서명을 그대로 두고 payload 만 바꾼다. 공격자가 자기 state 를 심으려는 상황이다.
    String[] parts = issued.getValue().split("\\.");
    String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
        """
        {"authorizationUri":"https://accounts.google.com/o/oauth2/v2/auth",\
        "clientId":"attacker","redirectUri":"https://evil.example.com",\
        "scopes":["profile"],"state":"attacker-state",\
        "additionalParameters":{},"attributes":{"registration_id":"google"}}\
        """.getBytes(StandardCharsets.UTF_8));

    MockHttpServletRequest callback = new MockHttpServletRequest();
    callback.setCookies(new Cookie("oauth2_auth_request", forgedPayload + "." + parts[1]));

    assertThat(authorizationRequestRepository.loadAuthorizationRequest(callback))
        .as("서명 검증 없이 받아들이면 공격자가 심은 state 로 로그인 CSRF 가 가능하다")
        .isNull();
  }

  @Test
  @DisplayName("깨진 쿠키는 오류가 아니라 '없음' 으로 처리한다")
  void treatsCorruptedCookieAsAbsent() {
    MockHttpServletRequest callback = new MockHttpServletRequest();
    callback.setCookies(new Cookie("oauth2_auth_request", "이건-base64-도-아니다"));

    // 예외가 새어 나가면 깨진 쿠키 하나로 500 이 난다. 로그인을 다시 하면 되는 일이다.
    assertThat(authorizationRequestRepository.loadAuthorizationRequest(callback)).isNull();
  }

  private MvcResult authorize() throws Exception {
    return mockMvc.perform(get("/oauth2/authorization/google")).andReturn();
  }

  private String stateOf(Cookie issued) throws Exception {
    JsonNode payload = new ObjectMapper()
        .readTree(Base64.getUrlDecoder().decode(issued.getValue().split("\\.")[0]));
    return payload.get("state").asText();
  }

  @Test
  @DisplayName("state 가 없는 콜백은 로그인 실패로 처리돼 프론트로 돌아간다")
  void redirectsToFrontendOnFailure() throws Exception {
    MvcResult result = mockMvc.perform(get("/login/oauth2/code/google").param("code", "dummy"))
        .andExpect(status().is3xxRedirection())
        .andReturn();

    assertThat(result.getResponse().getRedirectedUrl())
        .startsWith("http://localhost:5173/oauth/callback")
        .contains("error=OAUTH2_LOGIN_FAILED");
  }
}
