package com.getit.domain.user.dto;

import java.util.List;

/** 9.4 합격자 일괄 승격 결과. */
public record UserPromotionResult(
    int promotedCount,
    int skippedCount,
    List<PromotionSkipResult> skipped
) { }
