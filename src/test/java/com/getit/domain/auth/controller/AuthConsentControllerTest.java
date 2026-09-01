package com.getit.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** POST /api/auth/consent — 개인정보 수집·이용 동의 (이슈 #203) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthConsentControllerTest {

  private static final String CONSENT_PATH = "/api/auth/consent";
  private static final String ME_PATH = "/api/auth/me";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private UserRepository userRepository;

  private User signUp(String providerId, String email) {
    return userRepository.saveAndFlush(User.createGuest(providerId, email, "구글이름", null));
  }

  private String bearerFor(User user) {
    return "Bearer " + jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
  }

  private String body(String privacyConsent) {
    return "{\"privacyConsent\": %s}".formatted(privacyConsent);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 인증 오류다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(post(CONSENT_PATH))
          .andExpect(result ->
              assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }
  }

  @Nested
  @DisplayName("동의 기록")
  class Recording {

    @Test
    @DisplayName("본문 없이 호출해도 동의 시각이 저장된다")
    void recordsConsentWithoutRequestBody() throws Exception {
      User guest = signUp("google-sub-consent", "consent@getit.com");
      assertThat(guest.getPrivacyConsentedAt()).isNull();

      mockMvc.perform(post(CONSENT_PATH).header("Authorization", bearerFor(guest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.privacyConsentedAt").exists());

      assertThat(userRepository.findById(guest.getId()).orElseThrow().getPrivacyConsentedAt())
          .isNotNull();
    }

    @Test
    @DisplayName("본문에 true 를 담아 보내도 동작한다")
    void recordsConsentWithRequestBody() throws Exception {
      User guest = signUp("google-sub-consent-body", "consent-body@getit.com");

      mockMvc.perform(post(CONSENT_PATH)
              .header("Authorization", bearerFor(guest))
              .contentType(MediaType.APPLICATION_JSON)
              .content(body("true")))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.privacyConsentedAt").exists());
    }

    @Test
    @DisplayName("다시 호출해도 최초 동의 시각은 바뀌지 않는다")
    void keepsFirstConsentedAt() throws Exception {
      User guest = signUp("google-sub-consent-twice", "consent-twice@getit.com");
      LocalDateTime firstConsentedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
      guest.consentToPrivacy(firstConsentedAt);
      userRepository.flush();

      mockMvc.perform(post(CONSENT_PATH).header("Authorization", bearerFor(guest)))
          .andExpect(status().isOk());

      assertThat(userRepository.findById(guest.getId()).orElseThrow().getPrivacyConsentedAt())
          .isEqualTo(firstConsentedAt);
    }

    @Test
    @DisplayName("false 를 보내면 400 이고 동의로 기록되지 않는다")
    void rejectsFalse() throws Exception {
      User guest = signUp("google-sub-consent-false", "consent-false@getit.com");

      mockMvc.perform(post(CONSENT_PATH)
              .header("Authorization", bearerFor(guest))
              .contentType(MediaType.APPLICATION_JSON)
              .content(body("false")))
          .andExpect(status().isBadRequest());

      assertThat(userRepository.findById(guest.getId()).orElseThrow().getPrivacyConsentedAt())
          .isNull();
    }
  }

  @Nested
  @DisplayName("GET /api/auth/me")
  class Me {

    @Test
    @DisplayName("동의 전에는 privacyConsentedAt 이 비어 있다")
    void hasNoConsentedAtBeforeAgreement() throws Exception {
      User guest = signUp("google-sub-me-consent", "me-consent@getit.com");

      mockMvc.perform(get(ME_PATH).header("Authorization", bearerFor(guest)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.privacyConsentedAt").doesNotExist());
    }
  }
}
