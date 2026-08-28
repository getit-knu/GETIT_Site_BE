package com.getit.domain.lecture.service;

public record WeeklySubmissionStat(
    Long lectureId,
    int week,
    String title,
    long submittedCount
) { }
