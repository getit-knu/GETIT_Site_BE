package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;

/**
 * 관리자 사용자 목록 · 수정 결과. (API 명세서 9.1 · 9.2)
 *
 * <p>와이어프레임의 "권한" · "소속 분류" 두 컬럼은 {@code role} 하나로 통합했다
 * (GUEST=비회원 · MEMBER=부원 · ADMIN=운영진). {@code group} 은 미배정이면 null 이다.
 *
 * <p>{@code phoneNumber} 는 지원서에서 수집해 승격(9.4) 때 복사된 값이라, 승격을 거치지 않은
 * GUEST 는 비어 있다. 개인정보이므로 이 관리자 응답에만 싣는다 — 공개 API 에는 넣지 않는다
 * (이슈 #182).
 */
public record UserSummary(
    Long id,
    String name,
    String email,
    String phoneNumber,
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
        user.getPhoneNumber(),
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
