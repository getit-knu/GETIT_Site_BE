package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;

/** 서류/최종 합불 일괄 처리 결과. (API 명세서 7.4 일괄 처리) */
public record BulkDecisionResult(
    int updatedCount,
    ApplicationStatus status
) { }
