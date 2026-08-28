package com.getit.domain.setting.staff.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 운영진 구분. (API 명세서 10.21)
 *
 * <p>{@code label} 은 화면 표기용 한글 값이다. 2.3 공개 API 응답의 {@code sectionName} 으로
 * 쓴다 ({@code Role}·{@code UserStatus} 와 동일한 패턴).
 */
@Getter
@RequiredArgsConstructor
public enum StaffSection {

  EXECUTIVE("회장단"),
  SW("SW 운영진"),
  STARTUP("창업 운영진");

  private final String label;
}
