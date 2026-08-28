package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;
import java.util.List;

/** 2.3 section 별 운영진 그룹. 소속 운영진이 없어도 section 자체는 빈 배열로 내려준다. */
public record StaffSectionGroup(
    StaffSection section,
    String sectionName,
    List<PublicStaffResult> staffs
) {

  public static StaffSectionGroup of(StaffSection section, List<PublicStaffResult> staffs) {
    return new StaffSectionGroup(section, section.getLabel(), staffs);
  }
}
