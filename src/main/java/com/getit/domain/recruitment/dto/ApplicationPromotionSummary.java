package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.Application;

/**
 * 9.4 승격에 필요한 지원서 정보. (작업 분할 계획 4.2 크로스 도메인 계약)
 *
 * <p>{@code user} 도메인이 {@code Application} 엔티티를 직접 보지 못하므로
 * {@code ApplicationQueryService} 를 거쳐 이 record 로만 받는다.
 *
 * <p>{@code collegeId} · {@code majorId} 는 지원서 제출 흐름에 아직 값이 채워지지 않아
 * (College · Major 마스터 데이터 미연동, 이슈 #38 논의 필요 사항 참고) 현재는 항상 null 이다.
 * 그래도 필드는 그대로 넘긴다 — 나중에 연동되면 소비자(user)가 자기 소유의 College · Major
 * 테이블로 이름을 조회해서 쓸 수 있다.
 */
public record ApplicationPromotionSummary(
    Long applicationId,
    Long userId,
    String phoneNumber,
    Long collegeId,
    Long majorId,
    Integer studentYear,
    String studentNumber
) {

  public static ApplicationPromotionSummary from(Application application) {
    return new ApplicationPromotionSummary(
        application.getId(),
        application.getUserId(),
        application.getPhoneNumber(),
        application.getCollegeId(),
        application.getMajorId(),
        application.getGrade(),
        application.getStudentNumber()
    );
  }
}
