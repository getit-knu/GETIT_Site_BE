package com.getit.domain.lecture.service;

import java.time.LocalDateTime;

public record OngoingLectureStat(
    Long lectureId,
    String title,
    Long subCategoryId,
    LocalDateTime deadline,
    long submittedCount
) { }
