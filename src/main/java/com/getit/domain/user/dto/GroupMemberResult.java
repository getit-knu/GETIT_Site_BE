package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;

/** 조원 요약. (API 명세서 9.6) */
public record GroupMemberResult(
    Long userId,
    String name,
    String major,
    Role role,
    String roleLabel
) {

  public static GroupMemberResult from(User user) {
    return new GroupMemberResult(
        user.getId(),
        user.getName(),
        user.getMajor(),
        user.getRole(),
        user.getRole().getLabel()
    );
  }
}
