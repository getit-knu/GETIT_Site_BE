package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 모집 일정. Generation 과 1:1 이며 별도 엔티티로 분리한다. (작업 분할 계획 1.3 · API 명세서 6.1 · 6.2)
 *
 * <p>interviewEndAt 은 요청으로 받지 않는다. totalEndAt 과 항상 같은 값으로 서버가 채운다. (API 명세서 4.4)
 * 날짜 순서 검증(총 기간 · 서류 기간 · 면접 시작일)은 서비스 레이어에서 수행한다.
 */
@Entity
@Table(name = "recruitment_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitmentSchedule extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Long generationId;

  @Column(nullable = false)
  private LocalDateTime totalStartAt;

  @Column(nullable = false)
  private LocalDateTime totalEndAt;

  @Column(nullable = false)
  private LocalDateTime documentStartAt;

  @Column(nullable = false)
  private LocalDateTime documentEndAt;

  @Column(nullable = false)
  private LocalDateTime interviewStartAt;

  /** 항상 totalEndAt 과 동일하다. */
  @Column(nullable = false)
  private LocalDateTime interviewEndAt;

  @Builder(access = AccessLevel.PRIVATE)
  private RecruitmentSchedule(
      Long generationId,
      LocalDateTime totalStartAt,
      LocalDateTime totalEndAt,
      LocalDateTime documentStartAt,
      LocalDateTime documentEndAt,
      LocalDateTime interviewStartAt
  ) {
    this.generationId = generationId;
    this.totalStartAt = totalStartAt;
    this.totalEndAt = totalEndAt;
    this.documentStartAt = documentStartAt;
    this.documentEndAt = documentEndAt;
    this.interviewStartAt = interviewStartAt;
    this.interviewEndAt = totalEndAt;
  }

  public static RecruitmentSchedule create(
      Long generationId,
      LocalDateTime totalStartAt,
      LocalDateTime totalEndAt,
      LocalDateTime documentStartAt,
      LocalDateTime documentEndAt,
      LocalDateTime interviewStartAt
  ) {
    return RecruitmentSchedule.builder()
        .generationId(generationId)
        .totalStartAt(totalStartAt)
        .totalEndAt(totalEndAt)
        .documentStartAt(documentStartAt)
        .documentEndAt(documentEndAt)
        .interviewStartAt(interviewStartAt)
        .build();
  }

  /**
   * 모집 단계 판정. (API 명세서 2.8 · 3.1)
   *
   * <p>{@code interviewEndAt} 은 생성 · 수정 시 항상 {@code totalEndAt} 과 동일하게 맞춰지므로,
   * 명세서의 {@code FINAL_ANNOUNCED} 조건({@code now > totalEndAt})은 {@code now.isAfter(interviewEndAt)}
   * 과 같다. 활성 기수가 없는 {@code CLOSED} 는 이 메서드가 호출되는 시점(활성 기수 존재)에는
   * 해당하지 않아 호출자가 별도로 처리한다.
   */
  public RecruitmentPhase resolvePhase(LocalDateTime now) {
    if (now.isBefore(documentStartAt)) {
      return RecruitmentPhase.BEFORE_OPEN;
    }
    if (!now.isAfter(documentEndAt)) {
      return RecruitmentPhase.DOCUMENT_OPEN;
    }
    if (now.isBefore(interviewStartAt)) {
      return RecruitmentPhase.DOCUMENT_REVIEW;
    }
    if (!now.isAfter(interviewEndAt)) {
      return RecruitmentPhase.INTERVIEW;
    }
    return RecruitmentPhase.FINAL_ANNOUNCED;
  }

  /** 6.2 PUT. interviewEndAt 은 totalEndAt 으로 재동기화한다. */
  public void update(
      LocalDateTime totalStartAt,
      LocalDateTime totalEndAt,
      LocalDateTime documentStartAt,
      LocalDateTime documentEndAt,
      LocalDateTime interviewStartAt
  ) {
    this.totalStartAt = totalStartAt;
    this.totalEndAt = totalEndAt;
    this.documentStartAt = documentStartAt;
    this.documentEndAt = documentEndAt;
    this.interviewStartAt = interviewStartAt;
    this.interviewEndAt = totalEndAt;
  }
}
