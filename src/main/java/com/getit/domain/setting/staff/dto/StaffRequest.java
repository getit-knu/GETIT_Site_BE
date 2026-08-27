package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 운영진 추가 · 수정 요청. (API 명세서 10.21)
 *
 * <p>{@code order} 는 받지 않는다 — 생성 시 서버가 자동으로 다음 순번을 매기고, 순서 변경은
 * 10.22 전용 엔드포인트로만 한다.
 *
 * <p>{@code generationNo} 는 현재 활성 기수와 일치해야 한다.
 */
public record StaffRequest(
    Long userId,
    @NotBlank String name,
    @NotBlank String staffRole,
    @NotNull StaffSection section,
    @NotBlank String department,
    String introduction,
    Long fileId,
    @NotNull Integer generationNo
) { }
