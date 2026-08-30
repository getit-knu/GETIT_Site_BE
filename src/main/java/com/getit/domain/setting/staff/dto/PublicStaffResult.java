package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.Staff;

/**
 * 2.3 공개 운영진 카드 하나. 관리자용 {@code StaffResult} 와 달리 {@code userId}·
 * {@code generationNo} 등 내부 관리용 필드는 빼고 공개해도 되는 정보만 담는다.
 */
public record PublicStaffResult(
    Long id,
    String name,
    String staffRole,
    String department,
    String introduction,
    String githubUrl,
    String instagramUrl,
    String profileImageUrl,
    Integer order
) {

  /** {@code StaffResult} 와 동일한 기본 문구. */
  private static final String DEFAULT_INTRODUCTION = "한줄 소개를 작성해주세요";

  public static PublicStaffResult of(Staff staff, String profileImageUrl) {
    return new PublicStaffResult(
        staff.getId(),
        staff.getName(),
        staff.getStaffRole(),
        staff.getDepartment(),
        staff.getIntroduction() != null ? staff.getIntroduction() : DEFAULT_INTRODUCTION,
        staff.getGithubUrl(),
        staff.getInstagramUrl(),
        profileImageUrl,
        staff.getOrder()
    );
  }
}
