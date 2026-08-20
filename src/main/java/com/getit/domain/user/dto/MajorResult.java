package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Major;

/** 전공 조회 결과. (API 명세서 2.7) */
public record MajorResult(
    Long id,
    Long collegeId,
    String name
) {

  public static MajorResult from(Major major) {
    return new MajorResult(major.getId(), major.getCollegeId(), major.getName());
  }
}
