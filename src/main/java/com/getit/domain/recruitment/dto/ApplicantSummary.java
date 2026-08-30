package com.getit.domain.recruitment.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;

/**
 * 지원자 목록 한 줄. (7.1)
 *
 * @param college 단과대학 이름. 지원자가 고르지 않았거나 마스터 데이터에서 찾지 못하면 {@code null}
 * @param grade 학년
 */
public record ApplicantSummary(
    Long id,
    String name,
    String studentNumber,
    String college,
    Integer grade,
    ApplicationStatus status,
    LocalDateTime submittedAt
) {

  /**
   * @param collegeNames 단과대학 id → 이름. 목록 전체를 한 번에 조회한 결과를 넘긴다.
   *                     행마다 조회하면 N+1 이 된다 (이슈 #142)
   */
  public static ApplicantSummary from(Application application, Map<Long, String> collegeNames) {
    // 임시저장 단계에서는 소속을 비워둘 수 있다. 그리고 Map.of() 같은 불변 맵은
    // null 키로 조회하면 NPE 를 던지므로, 넘기기 전에 걸러낸다.
    Long collegeId = application.getCollegeId();
    String collegeName = collegeId == null ? null : collegeNames.get(collegeId);

    return new ApplicantSummary(
        application.getId(),
        application.getName(),
        application.getStudentNumber(),
        collegeName,
        application.getGrade(),
        application.getStatus(),
        application.getSubmittedAt()
    );
  }
}
