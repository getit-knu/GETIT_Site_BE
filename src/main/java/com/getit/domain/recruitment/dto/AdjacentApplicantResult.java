package com.getit.domain.recruitment.dto;

/**
 * 지원자 순차탐색 결과. (API 명세서 7.5)
 *
 * <p>7.1 목록과 동일한 필터·정렬 기준으로 나열했을 때 현재 지원서의 앞뒤 지원서 id 다.
 * 더 이상 이전 · 다음이 없으면 해당 값은 null 이다.
 */
public record AdjacentApplicantResult(
    Long previousApplicationId,
    Long nextApplicationId
) { }
