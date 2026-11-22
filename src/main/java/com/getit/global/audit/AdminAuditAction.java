package com.getit.global.audit;

/** 개인정보를 꺼내는 어드민 작업. 감사 기록에 남긴다. (이슈 #237) */
public enum AdminAuditAction {
  /** 부원 전체 번호 단건 조회. 대상은 사용자 id. */
  VIEW_USER_PHONE,
  /** 지원자 전체 번호 단건 조회. 대상은 지원서 id. */
  VIEW_APPLICANT_PHONE,
  /** 부원 엑셀 내려받기, 연락처 없이. 건수는 행 수. */
  EXPORT_USERS,
  /** 부원 엑셀 내려받기, 연락처 포함. 건수는 행 수. */
  EXPORT_USERS_WITH_PHONE
}
