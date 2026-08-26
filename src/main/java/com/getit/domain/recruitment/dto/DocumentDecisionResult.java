package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;

/** 서류 합불 처리 결과. (API 명세서 7.4) */
public record DocumentDecisionResult(
    Long applicationId,
    ApplicationStatus status
) { }
