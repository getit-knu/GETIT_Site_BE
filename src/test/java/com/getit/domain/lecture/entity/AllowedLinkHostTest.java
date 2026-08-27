package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AllowedLinkHostTest {

  @Test
  @DisplayName("서브도메인 미허용 호스트: 정확히 일치해야 허용된다")
  void allowsExactHostOnly() {
    assertThat(AllowedLinkHost.isAllowed("github.com")).isTrue();
    assertThat(AllowedLinkHost.isAllowed("sub.github.com")).isFalse();
  }

  @Test
  @DisplayName("서브도메인 허용 호스트: 서브도메인도 허용된다")
  void allowsSubdomainForVercel() {
    assertThat(AllowedLinkHost.isAllowed("vercel.app")).isTrue();
    assertThat(AllowedLinkHost.isAllowed("my-project.vercel.app")).isTrue();
  }

  @Test
  @DisplayName("대소문자를 구분하지 않는다")
  void isCaseInsensitive() {
    assertThat(AllowedLinkHost.isAllowed("GitHub.com")).isTrue();
  }

  @Test
  @DisplayName("화이트리스트에 없는 호스트는 거부된다")
  void rejectsUnlistedHost() {
    assertThat(AllowedLinkHost.isAllowed("evil.com")).isFalse();
  }
}
