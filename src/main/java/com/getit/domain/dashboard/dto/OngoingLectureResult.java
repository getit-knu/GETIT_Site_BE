package com.getit.domain.dashboard.dto;

import java.time.LocalDate;

/** 진행 중 강의. (API 명세서 5.5) */
public record OngoingLectureResult(
    Long id,
    String title,
    String subCategoryName,
    LocalDate deadline,
    long submittedCount,
    long totalCount
) { }
