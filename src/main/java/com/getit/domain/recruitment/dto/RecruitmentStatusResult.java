package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.RecruitmentPhase;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

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

  /**
   * 활성 기수의 모집 일정. {@code CLOSED} 면 일정 자체가 없어 null 이다.
   *
   * <p>{@code LocalDateTime} 은 오프셋 없이 직렬화되지만, 명세서 0.4 의 DateTime 규약은
   * {@code +09:00} 오프셋을 요구한다. 서울 시간대의 {@code OffsetDateTime} 으로 변환해서
   * 클라이언트가 시간대를 추론하지 않게 한다({@code SubmissionResult}와 동일 패턴, PR #86
   * Copilot 리뷰 지적).
   */
  public record ScheduleWindow(
      OffsetDateTime totalStartAt,
      OffsetDateTime totalEndAt,
      OffsetDateTime documentStartAt,
      OffsetDateTime documentEndAt,
      OffsetDateTime interviewStartAt,
      OffsetDateTime interviewEndAt
  ) {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    public static ScheduleWindow from(RecruitmentSchedule schedule) {
      return new ScheduleWindow(
          toOffsetDateTime(schedule.getTotalStartAt()),
          toOffsetDateTime(schedule.getTotalEndAt()),
          toOffsetDateTime(schedule.getDocumentStartAt()),
          toOffsetDateTime(schedule.getDocumentEndAt()),
          toOffsetDateTime(schedule.getInterviewStartAt()),
          toOffsetDateTime(schedule.getInterviewEndAt())
      );
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
      return dateTime.atZone(SEOUL).toOffsetDateTime();
    }
  }
}
