package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import java.time.LocalDateTime;

/** 모집 일정 조회 · 저장 결과. (API 명세서 6.1 · 6.2) */
public record RecruitmentScheduleResult(
    Long generationId,
    Integer generationNo,
    Integer year,
    LocalDateTime totalStartAt,
    LocalDateTime totalEndAt,
    LocalDateTime documentStartAt,
    LocalDateTime documentEndAt,
    LocalDateTime interviewStartAt,
    LocalDateTime interviewEndAt
) {

  public static RecruitmentScheduleResult of(GenerationSummary generation, RecruitmentSchedule schedule) {
    return new RecruitmentScheduleResult(
        generation.id(),
        generation.generationNo(),
        generation.year(),
        schedule.getTotalStartAt(),
        schedule.getTotalEndAt(),
        schedule.getDocumentStartAt(),
        schedule.getDocumentEndAt(),
        schedule.getInterviewStartAt(),
        schedule.getInterviewEndAt()
    );
  }
}
