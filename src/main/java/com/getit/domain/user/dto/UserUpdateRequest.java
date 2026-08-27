package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;

/**
 * 사용자 권한 · 그룹 · 기수 변경 요청. (API 명세서 9.2)
 *
 * <p>변경할 필드만 보낸다(partial update) — 세 필드 모두 nullable 이고, null 인 필드는
 * 그대로 둔다. 그래서 다른 요청 DTO 와 달리 {@code @NotNull} 을 걸지 않는다.
 */
public record UserUpdateRequest(
    Role role,
    Long groupId,
    Integer generationNo
) { }
