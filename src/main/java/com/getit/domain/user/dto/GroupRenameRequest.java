package com.getit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 조 이름 수정 요청. (API 명세서 9.8) */
public record GroupRenameRequest(
    @NotBlank @Size(max = 50) String name
) { }
