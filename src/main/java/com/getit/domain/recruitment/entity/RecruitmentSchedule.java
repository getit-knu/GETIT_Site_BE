package com.getit.domain.recruitment.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.DynamicUpdate;
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
/**
 * 바뀐 컬럼만 UPDATE 한다. (PR #173 리뷰 지적)
 *
 * <p>이 행에는 성격이 다른 두 갈래의 쓰기가 붙는다 — 일정 저장(6.2, 홈 일괄 저장)과
 * 지원 스위치({@code applyEnabled})다. Hibernate 의 기본 UPDATE 는 바꾸지 않은 컬럼까지
 * 함께 쓰기 때문에, 둘이 겹치면 나중에 커밋한 쪽이 상대가 방금 바꾼 값을 자기가 읽어 둔
 * 옛 값으로 되돌린다.
 *
 * <p>스위치는 사고가 났을 때 내리는 것이라, 그 사이 누가 일정을 저장했다는 이유로 조용히
 * 다시 올라가면 안 된다. 두 갈래가 건드리는 컬럼이 서로 겹치지 않으므로, 바뀐 컬럼만
 * 쓰게 하면 서로를 덮지 않는다.
 *
 * <p>{@code @Version} 대신 이 방법을 쓴다. 낙관적 잠금은 일정 저장끼리의 경합까지 잡아주지만
 * 컬럼 추가(마이그레이션)와 409 처리가 따라온다. 지금 막아야 하는 것은 성격이 다른 두 쓰기가
 * 서로를 지우는 것이고, 그건 컬럼을 나누는 것으로 충분하다.
 */
@Entity
@Table(name = "recruitment_schedule")
@DynamicUpdate
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

  /**
   * 운영진이 손으로 여닫는 지원 스위치. (이슈 #170)
   *
   * <p>일정과 별개다. 일정은 "언제 여는가"이고 이 값은 "지금 열어 두는가"다. 서류 기간
   * 중이라도 이걸 내리면 지원이 막힌다. 일정 값은 그대로 남으므로 공개 화면의 D-day 와
   * 일정 표시는 망가지지 않는다.
   *
   * <p>기본은 열림이다. 끄는 것은 사고가 났을 때뿐이라, 켜 두는 쪽이 기본이어야 한다.
   */
  @Column(nullable = false)
  private boolean applyEnabled = true;

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
   * <p>명세서 표는 INTERVIEW 상한을 {@code interviewEndAt} 으로, FINAL_ANNOUNCED 조건을
   * {@code now > totalEndAt} 으로 각각 적는다. 두 값은 생성 · 수정 시 항상 같게 맞춰지므로
   * (PR #39 리뷰) 굳이 두 필드를 따로 안 쓰고 {@code totalEndAt} 하나로 두 조건을 표현한다.
   * 활성 기수가 없는 {@code CLOSED} 는 이 메서드가 호출되는 시점(활성 기수 존재)에는 해당하지
   * 않아 호출자가 별도로 처리한다.
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
    if (!now.isAfter(totalEndAt)) {
      return RecruitmentPhase.INTERVIEW;
    }
    return RecruitmentPhase.FINAL_ANNOUNCED;
  }

  /** 지원을 여닫는다. 일정은 건드리지 않는다. (이슈 #170) */
  public void changeApplyEnabled(boolean applyEnabled) {
    this.applyEnabled = applyEnabled;
  }

  /** 지금 지원을 받을 수 있는 상태인지. 일정과 스위치를 함께 본다. */
  public boolean acceptsApplicationAt(LocalDateTime now) {
    return applyEnabled && resolvePhase(now) == RecruitmentPhase.DOCUMENT_OPEN;
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
