package com.getit.domain.recruitment.dto;

import java.time.LocalDate;

/** 합격 이후 다음 단계 안내. (API 명세서 3.5) */
public record NextStep(
    String type,
    String message,
    LocalDate periodStart,
    LocalDate periodEnd
) { }
