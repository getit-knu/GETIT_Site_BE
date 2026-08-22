package com.getit.domain.recruitment.dto;

/**
 * 지원서 기본 정보. (API 명세서 3.1 · 3.2)
 *
 * <p>3.1 에서는 {@code User} 값으로 채운 프리필로, 3.2 에서는 {@code Application} 에 저장된 값으로 쓴다.
 * {@code collegeId}·{@code majorId} 는 College · Major 마스터 데이터(2.6 · 2.7)가 아직 없어
 * 당분간 항상 {@code null} 이다 (이슈 #38 논의 필요 사항 참고).
 *
 * <p>{@code studentNumber} 는 PR #46 리뷰 지적으로 추가했다 — {@code Application} 엔티티 · 컬럼은
 * 이슈 #39 에서 이미 추가했지만 이 값을 실제로 받는 필드가 없어 항상 저장되지 않는 문제가 있었다.
 */
public record BasicInfo(
    String name,
    String email,
    String phoneNumber,
    Long collegeId,
    Long majorId,
    Integer grade,
    String studentNumber
) { }
