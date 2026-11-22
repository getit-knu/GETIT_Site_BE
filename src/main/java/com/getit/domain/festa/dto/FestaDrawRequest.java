package com.getit.domain.festa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 경품 추첨 요청. (이슈 #238)
 *
 * @param count 뽑을 인원. 비우면 5
 * @param additional 이미 뽑은 결과가 있을 때 그 뒤에 더 뽑는다. 연락이 안 되는 당첨자 대신 뽑을 때 쓴다.
 *     false 면 이미 뽑은 결과가 있을 때 거부한다 — 두 번 눌러서 당첨자가 늘어나는 것을 막는다
 */
public record FestaDrawRequest(@Min(1) @Max(50) Integer count, Boolean additional) {

  private static final int DEFAULT_COUNT = 5;

  public int countOrDefault() {
    return count == null ? DEFAULT_COUNT : count;
  }

  public boolean isAdditional() {
    return Boolean.TRUE.equals(additional);
  }
}
