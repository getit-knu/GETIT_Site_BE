package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.global.crypto.PhoneNumberMasker;

/**
 * 관리자 사용자 목록 · 수정 결과. (API 명세서 9.1 · 9.2)
 *
 * <p>와이어프레임의 "권한" · "소속 분류" 두 컬럼은 {@code role} 하나로 통합했다
 * (GUEST=비회원 · MEMBER=부원 · ADMIN=운영진). {@code group} 은 미배정이면 null 이다.
 *
 * <p>{@code phoneNumber} 는 지원서에서 수집해 승격(9.4) 때 복사된 값이라, 승격을 거치지 않은
 * GUEST 는 비어 있다. 개인정보이므로 이 관리자 응답에만 싣고, 그마저 {@code 010-****-5678} 로 가린다.
 * 전체 번호는 {@code GET /api/admin/users/{id}/phone} 으로 따로 받고 그때마다 기록한다 (이슈 #182, #237).
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
        PhoneNumberMasker.mask(user.getPhoneNumber()),
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
