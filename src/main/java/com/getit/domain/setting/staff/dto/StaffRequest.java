package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 운영진 추가 · 수정 요청. (API 명세서 10.21)
 *
 * <p>{@code order} 는 받지 않는다 — 생성 시 서버가 자동으로 다음 순번을 매기고, 순서 변경은
 * 10.22 전용 엔드포인트로만 한다.
 *
 * <p>{@code generationNo} 는 현재 활성 기수와 일치해야 한다.
 *
 * <p>문자열 필드는 엔티티 컬럼 길이(name·staffRole 50, department 100, introduction 255)에
 * 맞춰 {@code @Size} 로 미리 막는다 — 검증 없이 그대로 저장을 시도하면 DB 제약에서 500 으로
 * 실패했다(PR #82 Copilot 리뷰 지적).
 */
public record StaffRequest(
    Long userId,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 50) String staffRole,
    @NotNull StaffSection section,
    @NotBlank @Size(max = 100) String department,
    @Size(max = 255) String introduction,
    Long fileId,
    @NotNull Integer generationNo
) { }
