package com.getit.domain.setting.generation.dto;

import com.getit.domain.setting.generation.entity.Generation;

/** 진행 기수 · 연도 조회 · 저장 결과. (API 명세서 10.1 · 10.2) */
public record GenerationResult(
    Long id,
    Integer generationNo,
    Integer year,
    boolean isActive
) {

  public static GenerationResult from(Generation generation) {
    return new GenerationResult(
        generation.getId(),
        generation.getGenerationNo(),
        generation.getYear(),
        generation.isActive()
    );
  }
}
