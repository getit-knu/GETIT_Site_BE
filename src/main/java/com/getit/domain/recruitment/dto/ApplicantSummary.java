package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;

/**
 * 지원자 목록 한 행. (API 명세서 7.1)
 *
 * <p>{@code studentNumber} 는 3.1~3.5(지원자 본인)와 달리, 관리자가 지원자를 식별해야 하므로
 * 목록·상세(7.1·7.2)에는 노출한다.
 */
public record ApplicantSummary(
    Long id,
    String name,
    String studentNumber,
    ApplicationStatus status,
    LocalDateTime submittedAt
) {

  public static ApplicantSummary from(Application application) {
    return new ApplicantSummary(
        application.getId(),
        application.getName(),
        application.getStudentNumber(),
        application.getStatus(),
        application.getSubmittedAt()
    );
  }
}
