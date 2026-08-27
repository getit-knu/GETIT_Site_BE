package com.getit.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 합격자 일괄 승격 요청. (API 명세서 9.4)
 *
 * <p>{@code applicationIds} 가 없으면 해당 기수의 FINAL_PASS 전체가 대상이다.
 */
public record PromoteRequest(
    @NotNull Long generationId,
    List<Long> applicationIds
) { }
