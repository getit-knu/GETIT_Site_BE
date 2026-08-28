package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.RecruitmentPhase;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import java.time.LocalDateTime;

/** 2.8 공개 모집 상태 · D-day. */
public record RecruitmentStatusResult(
    Integer generationNo,
    Integer year,
    RecruitmentPhase phase,
    Long dDay,
    String message,
    boolean applyEnabled,
    ScheduleWindow schedule
) {

  /** 활성 기수의 모집 일정. {@code CLOSED} 면 일정 자체가 없어 null 이다. */
  public record ScheduleWindow(
      LocalDateTime totalStartAt,
      LocalDateTime totalEndAt,
      LocalDateTime documentStartAt,
      LocalDateTime documentEndAt,
      LocalDateTime interviewStartAt,
      LocalDateTime interviewEndAt
  ) {

    public static ScheduleWindow from(RecruitmentSchedule schedule) {
      return new ScheduleWindow(
          schedule.getTotalStartAt(),
          schedule.getTotalEndAt(),
          schedule.getDocumentStartAt(),
          schedule.getDocumentEndAt(),
          schedule.getInterviewStartAt(),
          schedule.getInterviewEndAt()
      );
    }
  }
}
