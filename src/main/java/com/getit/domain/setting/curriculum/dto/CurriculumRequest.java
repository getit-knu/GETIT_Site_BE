package com.getit.domain.setting.curriculum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 커리큘럼 추가 · 수정 요청. (API 명세서 10.11 · 10.12)
 *
 * <p>{@code generationId} 는 현재 활성 기수와 일치해야 한다 — 비활성(과거) 기수에 커리큘럼을
 * 추가 · 이동하는 것은 명세서에 근거가 없어 막는다.
 */
public record CurriculumRequest(
    @NotNull Long generationId,
    @NotBlank String title,
    @NotBlank String subtitle,
    @NotNull Integer order
) { }
