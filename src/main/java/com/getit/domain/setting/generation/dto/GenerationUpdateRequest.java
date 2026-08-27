package com.getit.domain.setting.generation.dto;

import jakarta.validation.constraints.NotNull;

/** 진행 기수 · 연도 저장 요청. (API 명세서 10.2) */
public record GenerationUpdateRequest(
    @NotNull Integer generationNo,
    @NotNull Integer year
) { }
