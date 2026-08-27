package com.getit.domain.user.dto;

/** 승격에서 제외된 지원서 하나. (API 명세서 9.4) */
public record PromotionSkipResult(
    Long applicationId,
    PromotionSkipReason reason
) { }
