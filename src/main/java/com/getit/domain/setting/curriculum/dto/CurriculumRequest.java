package com.getit.domain.setting.curriculum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 커리큘럼 추가 · 수정 요청. (API 명세서 10.11 · 10.12)
 *
 * <p>{@code generationId} 는 현재 활성 기수와 일치해야 한다 — 비활성(과거) 기수에 커리큘럼을
 * 추가 · 이동하는 것은 명세서에 근거가 없어 막는다.
 *
 * <p>{@code title} · {@code subtitle} 은 엔티티 컬럼 길이(100 · 255)에 맞춰 {@code @Size} 로
 * 미리 막고, {@code order} 는 {@code @Min(1)} 로 0 이하를 막는다 — 검증 없이 그대로 저장을
 * 시도하면 DB 제약에서 500 으로 실패했다(PR #78 Copilot 리뷰 지적).
 */
public record CurriculumRequest(
    @NotNull Long generationId,
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 255) String subtitle,
    @NotNull @Min(1) Integer order
) { }
