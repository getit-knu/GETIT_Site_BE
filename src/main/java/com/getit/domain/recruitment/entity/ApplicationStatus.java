package com.getit.domain.recruitment.entity;

/** 지원서 상태. (API 명세서 0.5) */
public enum ApplicationStatus {
  DRAFT,
  SUBMITTED,
  DOC_PASS,
  DOC_FAIL,
  FINAL_PASS,
  FINAL_FAIL;

  /**
   * 한글 라벨. {@code ApplicationService.statusLabel}(3.5 결과 조회)에 있던 매핑을 여기로 옮겼다 —
   * 7.6 엑셀 다운로드도 같은 매핑이 필요해서, 두 곳에 중복해서 두는 대신 enum 에 하나만 둔다
   * (PR #54 리뷰 지적 — 엑셀에 DOC_PASS 같은 원본 enum 이름이 그대로 노출되고 있었다).
   */
  public String label() {
    return switch (this) {
      case DRAFT -> "임시 저장";
      case SUBMITTED -> "심사 중";
      case DOC_PASS -> "서류 합격";
      case DOC_FAIL -> "서류 불합격";
      case FINAL_PASS -> "최종 합격";
      case FINAL_FAIL -> "최종 불합격";
    };
  }
}
