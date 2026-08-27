package com.getit.domain.user.dto;

/** 9.4 승격 대상에서 제외된 이유. (API 명세서 9.4 응답 skipped[].reason) */
public enum PromotionSkipReason {

  /** 이미 GUEST 가 아닌(MEMBER · ADMIN) 사용자. */
  ALREADY_MEMBER,

  /** 탈퇴(soft delete)했거나 존재하지 않는 사용자. */
  USER_WITHDRAWN,

  /** applicationIds 로 지정했지만 해당 기수의 FINAL_PASS 지원서가 아님. */
  NOT_FINAL_PASS
}
