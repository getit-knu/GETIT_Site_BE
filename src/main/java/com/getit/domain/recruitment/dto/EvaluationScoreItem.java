package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 점수 upsert 요청 원소. (API 명세서 7.3) */
public record EvaluationScoreItem(
    @NotNull Long criterionId,
    @NotNull @Min(0) Integer score
) { }
