package com.getit.domain.recruitment.dto;

/**
 * 지원서 기본 정보. (API 명세서 3.1 · 3.2)
 *
 * <p>3.1 에서는 {@code User} 값으로 채운 프리필로, 3.2 에서는 {@code Application} 에 저장된 값으로 쓴다.
 * {@code collegeId}·{@code majorId} 는 College · Major 마스터 데이터(2.6 · 2.7)가 아직 없어
 * 당분간 항상 {@code null} 이다 (이슈 #38 논의 필요 사항 참고).
 */
public record BasicInfo(
    String name,
    String email,
    String phoneNumber,
    Long collegeId,
    Long majorId,
    Integer grade
) { }
