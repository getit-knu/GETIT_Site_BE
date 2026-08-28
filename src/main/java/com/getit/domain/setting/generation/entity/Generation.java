package com.getit.domain.setting.generation.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기수. 지원서 · 강의 · 조 등 거의 모든 도메인이 FK 로 참조한다. (설계 명세서 2.2)
 *
 * <p>모집 일정(totalStartAt · documentStartAt 등)은 이 엔티티가 갖지 않는다.
 * {@code RecruitmentSchedule} 로 분리해 recruitment 패키지에서 관리한다.
 * 6.2 PUT /admin/recruitment/schedule 이 A 담당이기 때문이다.
 */
@Entity
@Table(name = "generation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Generation extends BaseTimeEntity {

  /**
   * 활성화 로직을 직렬화하는 잠금 행 전용 예약 기수 번호. 실제 기수로는 절대 쓰이지 않는다
   * ({@code GenerationUpdateRequest} 가 {@code @Positive} 로 0 이하 값을 항상 거부한다,
   * PR #76 Copilot 리뷰 지적). 다른 도메인이 {@code generationNo} 로 기수를 조회할 때 이 값을
   * 진짜 기수로 오인하지 않도록, 조회 계약({@code GenerationQueryService.findByGenerationNo})도
   * 이 상수를 참조해서 걸러낸다.
   */
  public static final int RESERVED_ACTIVATION_LOCK_GENERATION_NO = 0;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private Integer generationNo;

  /** year 는 SQL 예약어라 컬럼명을 분리한다. 응답 JSON 키는 필드명 그대로 year 다. */
  @Column(name = "generation_year", nullable = false)
  private Integer year;

  /**
   * 현재 진행 기수 여부. 전체에서 항상 1건만 true 여야 한다. (설계 명세서 4.5)
   * 단일성 보장은 DB 제약으로 표현할 수 없으므로 활성화 트랜잭션에서 서비스가 책임진다.
   */
  @Column(nullable = false)
  private boolean isActive;

  @Builder(access = AccessLevel.PRIVATE)
  private Generation(Integer generationNo, Integer year, boolean isActive) {
    this.generationNo = generationNo;
    this.year = year;
    this.isActive = isActive;
  }

  public static Generation create(Integer generationNo, Integer year) {
    return Generation.builder()
        .generationNo(generationNo)
        .year(year)
        .isActive(false)
        .build();
  }

  /** 진행 기수 · 연도 변경. (10.2 PUT /admin/setting/generation) */
  public void updateInfo(Integer generationNo, Integer year) {
    this.generationNo = generationNo;
    this.year = year;
  }

  /**
   * 활성화. 기존 활성 기수를 비활성화하는 것은 호출하는 서비스의 책임이다.
   * 두 작업이 하나의 트랜잭션 안에서 일어나야 활성 기수 단일성이 지켜진다.
   */
  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
