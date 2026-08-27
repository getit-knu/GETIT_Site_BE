package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;

/** 운영진 조회 · 저장 결과. (API 명세서 10.21) */
public record StaffResult(
    Long id,
    Long userId,
    String name,
    String staffRole,
    StaffSection section,
    String department,
    String introduction,
    String profileImageUrl,
    Integer order,
    Integer generationNo
) {

  /** introduction 이 비어 있으면 기본 문구를 채운다. */
  private static final String DEFAULT_INTRODUCTION = "한줄 소개를 작성해주세요";

  public static StaffResult of(Staff staff, String profileImageUrl) {
    return new StaffResult(
        staff.getId(),
        staff.getUserId(),
        staff.getName(),
        staff.getStaffRole(),
        staff.getSection(),
        staff.getDepartment(),
        staff.getIntroduction() != null ? staff.getIntroduction() : DEFAULT_INTRODUCTION,
        profileImageUrl,
        staff.getOrder(),
        staff.getGenerationNo()
    );
  }
}
