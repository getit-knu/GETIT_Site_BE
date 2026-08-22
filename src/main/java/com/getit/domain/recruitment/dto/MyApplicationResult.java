package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 내 지원서 조회 결과. (API 명세서 3.2)
 *
 * <p>{@code savedAt} 은 별도 컬럼이 아니라 {@code Application.updatedAt} 이다. 임시 저장(3.3)도
 * 매번 이 엔티티를 갱신하므로 "마지막으로 저장된 시각"과 같은 의미다.
 */
public record MyApplicationResult(
    Long id,
    Integer generationNo,
    ApplicationStatus status,
    BasicInfo basicInfo,
    List<ApplicationAnswerResult> answers,
    LocalDateTime savedAt,
    LocalDateTime submittedAt
) {

  public static MyApplicationResult of(
      Application application, Integer generationNo, List<ApplicationAnswerResult> answers
  ) {
    return new MyApplicationResult(
        application.getId(),
        generationNo,
        application.getStatus(),
        new BasicInfo(
            application.getName(),
            application.getEmail(),
            application.getPhoneNumber(),
            application.getCollegeId(),
            application.getMajorId(),
            application.getGrade(),
            application.getStudentNumber()
        ),
        answers,
        application.getUpdatedAt(),
        application.getSubmittedAt()
    );
  }
}
