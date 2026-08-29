package com.getit.domain.setting.curriculum.service;

/**
 * 커리큘럼 조회 결과 — 크로스 도메인 소비용 raw 데이터. (홈 통합 조회 2.1)
 *
 * <p>{@code CurriculumResult}(admin 응답)와 필드 구성이 같지만, 계약의 반환 타입은 소비자의
 * 응답 DTO 와 분리한다({@code FaqView}·{@code ProjectView} 등과 동일한 이유 — 관리자 응답 모양이
 * 바뀌어도 계약이 함께 깨지지 않게 하기 위함).
 */
public record CurriculumView(
    Long id,
    Integer order,
    String title,
    String subtitle
) { }
