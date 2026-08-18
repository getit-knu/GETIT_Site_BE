package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** 평가 기준 추가 · 수정 요청. (API 명세서 6.9 · 6.10) */
public record EvaluationCriterionRequest(
    @NotBlank String name,
    @NotBlank String guideline,
    @NotNull @Min(1) @Max(100) Integer maxScore
) { }
