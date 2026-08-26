package com.getit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 조 생성 요청. (API 명세서 9.7) */
public record GroupCreateRequest(
    @NotNull Long generationId,
    @NotBlank @Size(max = 50) String name
) { }
