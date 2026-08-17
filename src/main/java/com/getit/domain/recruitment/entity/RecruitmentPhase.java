package com.getit.domain.recruitment.entity;

/**
 * 모집 단계. (API 명세서 0.5 · 2.8)
 *
 * <p>{@code CLOSED}는 활성 기수가 없을 때다. 활성 기수가 있어야 판정 가능한 나머지 단계와
 * 달리 {@link RecruitmentSchedule} 만으로는 알 수 없어 {@code resolvePhase} 밖에서 판단한다.
 */
public enum RecruitmentPhase {
  BEFORE_OPEN,
  DOCUMENT_OPEN,
  DOCUMENT_REVIEW,
  INTERVIEW,
  FINAL_ANNOUNCED,
  CLOSED
}
