package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;

/**
 * 관리자 사용자 목록 · 수정 결과. (API 명세서 9.1 · 9.2)
 *
 * <p>와이어프레임의 "권한" · "소속 분류" 두 컬럼은 {@code role} 하나로 통합했다
 * (GUEST=비회원 · MEMBER=부원 · ADMIN=운영진). {@code group} 은 미배정이면 null 이다.
 */
public record UserSummary(
    Long id,
    String name,
    String email,
    String college,
    String major,
    Integer studentYear,
    Role role,
    String roleLabel,
    Integer generationNo,
    GroupSummary group,
    UserStatus status
) {

  public static UserSummary from(User user, GroupSummary group) {
    return new UserSummary(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getCollege(),
        user.getMajor(),
        user.getStudentYear(),
        user.getRole(),
        user.getRole().getLabel(),
        user.getGenerationNo(),
        group,
        user.getStatus()
    );
  }
}
