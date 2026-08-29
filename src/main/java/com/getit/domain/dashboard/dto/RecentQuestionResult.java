package com.getit.domain.dashboard.dto;

import java.time.OffsetDateTime;

/** 미확인 Q&A. (API 명세서 5.2) */
public record RecentQuestionResult(
    Long id,
    String authorName,
    String content,
    OffsetDateTime createdAt,
    String elapsedLabel,
    String lectureTitle
) { }
