package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.NotNull;

/** 서류 합불 처리 요청. (API 명세서 7.4) */
public record DocumentDecisionRequest(
    @NotNull Boolean passed
) { }
