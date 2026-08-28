package com.getit.domain.qna.service;

import java.time.LocalDateTime;

public record RecentQuestion(
    Long questionId,
    long authorId,
    String content,
    LocalDateTime createdAt,
    Long lectureId
) { }
