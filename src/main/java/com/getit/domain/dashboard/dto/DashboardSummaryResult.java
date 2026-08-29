package com.getit.domain.dashboard.dto;

/** 대시보드 상단 카운터 4종. (API 명세서 5.1) */
public record DashboardSummaryResult(
    long totalApplicants,
    long memberCount,
    long unEvaluatedAssignmentCount,
    long unansweredQuestionCount
) { }
