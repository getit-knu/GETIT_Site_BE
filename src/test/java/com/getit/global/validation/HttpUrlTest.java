package com.getit.global.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 화면이 {@code href} 에 그대로 넣는 주소를 검증한다.
 *
 * <p>이 규칙이 한 곳에 모여 있지 않으면 새 URL 필드가 생길 때 빠뜨린다. 실제로 운영진
 * 링크만 막고 프로젝트 · 강의 링크가 열려 있는 상태가 한동안 있었다 (이슈 #159).
 */
class HttpUrlTest {

  private static Validator validator;

  private record Holder(@HttpUrl String url) { }

  @BeforeAll
  static void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  private Set<ConstraintViolation<Holder>> validate(String url) {
    return validator.validate(new Holder(url));
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "javascript:alert(1)",
      "JavaScript:alert(1)",
      "data:text/html;base64,PHNjcmlwdD4=",
      "vbscript:msgbox(1)",
      "file:///etc/passwd",
      "//evil.example.com",
      "github.com/hong",
      " https://github.com/hong"
  })
  @DisplayName("http · https 가 아닌 주소는 거부한다")
  void rejectsNonHttpSchemes(String url) {
    assertThat(validate(url)).isNotEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "https://github.com/hong",
      "http://example.com",
      "https://youtu.be/abc?t=10"
  })
  @DisplayName("http · https 주소는 통과한다")
  void acceptsHttpSchemes(String url) {
    assertThat(validate(url)).isEmpty();
  }

  @ParameterizedTest
  @NullSource
  @DisplayName("null 은 통과한다 — 대체로 선택값이다")
  void acceptsNull(String url) {
    assertThat(validate(url)).isEmpty();
  }

  @Test
  @DisplayName("512자를 넘으면 거부한다")
  void rejectsTooLong() {
    assertThat(validate("https://example.com/" + "a".repeat(500))).isNotEmpty();
  }
}
