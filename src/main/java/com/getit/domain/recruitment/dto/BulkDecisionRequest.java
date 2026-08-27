package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 서류/최종 합불 일괄 처리 요청. (API 명세서 7.4 일괄 처리) */
public record BulkDecisionRequest(
    @NotEmpty List<Long> applicationIds,
    @NotNull ApplicationStatus status
) { }
