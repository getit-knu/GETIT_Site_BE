package com.getit.domain.setting.generation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 진행 기수 · 연도 저장 요청. (API 명세서 10.2)
 *
 * <p>{@code @Positive} 로 0 이하 값을 막는다 — {@code @NotNull} 만으로는 0·음수 기수·연도가
 * 그대로 저장됐다(PR #76 Copilot 리뷰 지적). 0 은 활성화 로직을 직렬화하는 데 쓰는 예약
 * 기수 번호({@link com.getit.domain.setting.generation.service.GenerationAdminService})이기도
 * 해서, 이 검증이 일반 요청이 그 값을 건드리지 못하게 막는 역할도 겸한다.
 */
public record GenerationUpdateRequest(
    @NotNull @Positive Integer generationNo,
    @NotNull @Positive Integer year
) { }
