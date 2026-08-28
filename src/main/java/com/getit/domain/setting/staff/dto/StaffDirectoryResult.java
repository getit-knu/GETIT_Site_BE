package com.getit.domain.setting.staff.dto;

import java.util.List;

/** 2.3 운영진 소개 응답. */
public record StaffDirectoryResult(
    List<StaffSectionGroup> sections
) { }
