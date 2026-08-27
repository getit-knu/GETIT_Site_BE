package com.getit.domain.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 조원 추가 요청. (API 명세서 9.10) */
public record GroupMemberAddRequest(
    @NotEmpty List<Long> userIds
) { }
