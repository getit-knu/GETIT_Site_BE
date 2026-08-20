package com.getit.domain.user.dto;

import com.getit.domain.user.entity.College;

/** 단과대학 조회 결과. (API 명세서 2.6) */
public record CollegeResult(
    Long id,
    String name
) {

  public static CollegeResult from(College college) {
    return new CollegeResult(college.getId(), college.getName());
  }
}
