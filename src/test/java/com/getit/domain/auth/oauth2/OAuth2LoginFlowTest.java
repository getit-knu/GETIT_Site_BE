package com.getit.domain.auth.oauth2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

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
