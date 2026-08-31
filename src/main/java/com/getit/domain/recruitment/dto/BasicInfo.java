package com.getit.domain.recruitment.dto;

/**
 * 지원서 기본 정보. (API 명세서 3.1 · 3.2)
 *
 * <p>3.1 에서는 {@code User} 값으로 채운 프리필로, 3.2 에서는 {@code Application} 에 저장된 값으로 쓴다.
 * <p>{@code collegeId}·{@code majorId} 는 저장되고 다시 읽힌다. 마스터 데이터(2.6 · 2.7)도
 * 시드되어 있고 승격(9.4)이 이름으로 바꿔 {@code User} 에 복사한다. 값이 비는 것은 지원서
 * 폼이 아직 id 를 담아 보내지 않기 때문이다 — 서버 쪽에 빠진 것은 없다 (이슈 #184).
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
