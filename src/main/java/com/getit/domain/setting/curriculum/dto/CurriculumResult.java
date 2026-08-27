package com.getit.domain.setting.curriculum.dto;

import com.getit.domain.setting.curriculum.entity.Curriculum;

/** 커리큘럼 조회 · 저장 결과. (API 명세서 10.10 ~ 10.13) */
public record CurriculumResult(
    Long id,
    Integer order,
    String title,
    String subtitle
) {

  public static CurriculumResult from(Curriculum curriculum) {
    return new CurriculumResult(
        curriculum.getId(),
        curriculum.getOrder(),
        curriculum.getTitle(),
        curriculum.getSubtitle()
    );
  }
}
