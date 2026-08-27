package com.getit.domain.lecture.entity;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 과제 제출 시 허용하는 링크 호스트 화이트리스트.
 *
 * <p>운영진이 제출물을 확인하려 링크를 클릭하므로, 임의 URL을 허용하면 피싱 링크가 섞일 위험이 있다.
 */
@Getter
@RequiredArgsConstructor
public enum AllowedLinkHost {

  GITHUB("github.com", false),
  GITLAB("gitlab.com", false),
  NOTION("notion.so", false),
  NOTION_SITE("notion.site", true),
  VERCEL("vercel.app", true),
  NETLIFY("netlify.app", true);

  private final String host;
  private final boolean allowSubdomain;

  public static boolean isAllowed(String host) {
    String lowerHost = host.toLowerCase();
    return Arrays.stream(values()).anyMatch(allowedHost -> allowedHost.matches(lowerHost));
  }

  private boolean matches(String lowerHost) {
    return lowerHost.equals(host) || (allowSubdomain && lowerHost.endsWith("." + host));
  }
}
