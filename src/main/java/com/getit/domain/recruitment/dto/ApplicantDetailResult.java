package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.global.crypto.PhoneNumberMasker;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 지원자 상세 조회 결과. (API 명세서 7.2)
 *
 * <p>평가 점수는 아직 {@code EvaluationScore} 엔티티가 없어(이슈 #6/7.3~7.4) 포함하지 않는다.
 *
 * <p>{@code studentNumber} 는 별도 최상위 필드가 아니라 {@code basicInfo} 안에 있다 — PR #46 에서
 * {@code BasicInfo} 에 {@code studentNumber} 를 추가하면서 {@code MyApplicationResult} 와 동일한
 * 모양으로 맞췄다.
 *
 * <p>{@code basicInfo.phoneNumber} 는 {@code 010-****-5678} 로 가린다. 전체 번호는
 * {@code GET /api/admin/recruitment/applications/{id}/phone} 으로 따로 받고 그때마다 기록한다 (이슈 #237).
 */
public record ApplicantDetailResult(
    Long id,
    ApplicationStatus status,
    BasicInfo basicInfo,
    List<ApplicationAnswerResult> answers,
    LocalDateTime submittedAt
) {

  public static ApplicantDetailResult of(Application application, List<ApplicationAnswerResult> answers) {
    return new ApplicantDetailResult(
        application.getId(),
        application.getStatus(),
        new BasicInfo(
            application.getName(),
            application.getEmail(),
            PhoneNumberMasker.mask(application.getPhoneNumber()),
            application.getCollegeId(),
            application.getMajorId(),
            application.getGrade(),
            application.getStudentNumber()
        ),
        answers,
        application.getSubmittedAt()
    );
  }
}
