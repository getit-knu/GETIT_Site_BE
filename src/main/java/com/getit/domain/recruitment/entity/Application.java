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
 * 이슈 #44 논의 필요 사항 참고.
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

  /**
   * 3.3 임시 저장. 기본 정보를 통째로 덮어쓴다.
   *
   * <p>{@code status != DRAFT} 일 때 호출해도 되는지는 서비스 레이어에서 막는다
   * (409 ALREADY_SUBMITTED, 명세서 3.3) — 다른 검증과 마찬가지로 이 엔티티는 검증 없이 그대로 담는다.
   */
  public void updateDraft(
      String name,
      String email,
      String phoneNumber,
      Long collegeId,
      Long majorId,
      Integer grade,
      String studentNumber
  ) {
    this.name = name;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.collegeId = collegeId;
    this.majorId = majorId;
    this.grade = grade;
    this.studentNumber = studentNumber;
  }

  /** 3.4 제출. 통과해야 할 검증은 전부 서비스 레이어에서 먼저 끝낸 뒤 호출된다. */
  public void submit(LocalDateTime submittedAt) {
    this.status = ApplicationStatus.SUBMITTED;
    this.submittedAt = submittedAt;
  }

  /**
   * 7.4 서류 합불 처리. {@code SUBMITTED} 상태에서만 호출할 수 있는지는 서비스 레이어에서 막는다
   * (다른 검증과 마찬가지로 이 엔티티는 검증 없이 그대로 담는다).
   *
   * <p>PR #51 리뷰 지적으로 {@code ApplicationStatus} 를 그대로 받던 시그니처를 좁혔다 — {@code submit()}
   * 처럼 상태값을 인자로 받지 않아야 잘못된 상태(예: DRAFT, FINAL_PASS)로 되돌리는 호출부 실수 자체가
   * 불가능해진다. "허용 값 검증은 서비스 책임"은 배점처럼 값의 범위에 적용할 때나 맞고, 상태 전이는
   * 엔티티가 타입으로 막는 게 맞다는 리뷰 의견을 따랐다.
   */
  public void decideDocumentResult(boolean passed) {
    this.status = passed ? ApplicationStatus.DOC_PASS : ApplicationStatus.DOC_FAIL;
  }
}
