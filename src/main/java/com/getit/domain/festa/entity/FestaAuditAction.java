package com.getit.domain.festa.entity;

/** 전화번호를 다루는 어드민 작업. 감사 기록에 남긴다. */
public enum FestaAuditAction {
  EXPORT_PARTICIPANTS,
  DESTROY_PHONES,
  AUTO_DESTROY_PHONES,
  RESET_ALL,
  /** 서버 추첨. 건수는 이번에 뽑은 인원. (이슈 #238) */
  DRAW_WINNERS,
  /** 당첨자 전체 번호 조회. 건수는 보여준 인원. (이슈 #238) */
  VIEW_WINNERS
}
