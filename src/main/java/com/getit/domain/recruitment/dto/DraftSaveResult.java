package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;

/** 임시 저장 결과. (API 명세서 3.3) */
public record DraftSaveResult(
    Long id,
    ApplicationStatus status,
    LocalDateTime savedAt
) { }
