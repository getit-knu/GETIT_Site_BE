package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 지원서. (API 명세서 3.1 · 3.2)
 *
 * <p>기본 정보(name · email · phoneNumber · collegeId · majorId · grade · studentNumber)는 지원서
 * 제출 시점 값을 그대로 담는다. {@code User} 의 값과 다를 수 있다 — 지원 당시 정보이기 때문이다.
 * 합격자는 9.4 승격 시 이 값이 {@code User} 로 복사된다.
 *
 * <p>{@code studentNumber} 는 명세서 3.1 ~ 3.4 의 JSON 응답 · 요청 본문에는 나와 있지 않지만,
 * {@code User.studentNumber} 의 기존 주석("지원서 기본 정보에서 수집한다")과 PR #39 리뷰(학번이
 * 지원서에서 수집돼야 한다는 지적)에 따라 엔티티 · 스키마에는 먼저 반영한다. API 응답에 노출할지는
 * 3.3 ~ 3.5 구현 시 다시 확인한다.
 *
 * <p>이번 이슈(3.1 · 3.2)는 조회만 다뤄서 저장 · 제출 메서드는 아직 없다. 3.3 ~ 3.5 에서 추가한다.
 */
@Entity
@Table(name = "application", uniqueConstraints = {
    @UniqueConstraint(name = "uk_application_user_generation", columnNames = {"user_id", "generation_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Long generationId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private ApplicationStatus status;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(length = 20)
  private String phoneNumber;

  /** College 마스터 데이터(2.6)가 아직 없어 항상 null 이다 (이슈 논의 필요 사항 참고). */
  @Column
  private Long collegeId;

  /** Major 마스터 데이터(2.7)가 아직 없어 항상 null 이다 (이슈 논의 필요 사항 참고). */
  @Column
  private Long majorId;

  @Column
  private Integer grade;

  /**
   * 학번. {@code User.STUDENT_NUMBER_PATTERN}(년도 4자리 + 고유번호 6자리)과 같은 형식이다.
   * DB CHECK 제약도 {@code User} 와 동일하게 건다 (마이그레이션 참고).
   */
  @Column(columnDefinition = "CHAR(10)")
  private String studentNumber;

  /** 제출 전(DRAFT)에는 null 이다. */
  @Column
  private LocalDateTime submittedAt;

  @Builder(access = AccessLevel.PRIVATE)
  private Application(
      Long userId,
      Long generationId,
      ApplicationStatus status,
      String name,
      String email,
      String phoneNumber,
      Long collegeId,
      Long majorId,
      Integer grade,
      String studentNumber,
      LocalDateTime submittedAt
  ) {
    this.userId = userId;
    this.generationId = generationId;
    this.status = status;
    this.name = name;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.collegeId = collegeId;
    this.majorId = majorId;
    this.grade = grade;
    this.studentNumber = studentNumber;
    this.submittedAt = submittedAt;
  }

  /** 3.3 임시 저장 시 최초 생성한다. 저장 시점엔 아직 DRAFT 다. */
  public static Application createDraft(
      Long userId,
      Long generationId,
      String name,
      String email,
      String phoneNumber,
      Long collegeId,
      Long majorId,
      Integer grade,
      String studentNumber
  ) {
    return Application.builder()
        .userId(userId)
        .generationId(generationId)
        .status(ApplicationStatus.DRAFT)
        .name(name)
        .email(email)
        .phoneNumber(phoneNumber)
        .collegeId(collegeId)
        .majorId(majorId)
        .grade(grade)
        .studentNumber(studentNumber)
        .build();
  }
}
