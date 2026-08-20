package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;

/** 지원서 제출 결과. (API 명세서 3.4) */
public record SubmitResult(
    Long id,
    ApplicationStatus status,
    LocalDateTime submittedAt
) { }
