package com.getit.domain.setting.curriculum.entity;

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
 * 커리큘럼 항목. 홈 화면에 노출되는 기수별 커리큘럼 카드 하나를 나타낸다. (API 명세서 10.10 ~ 10.13)
 *
 * <p>{@code order} 는 {@code ApplicationQuestion} 과 달리 클라이언트가 요청 바디로 직접 값을
 * 보낸다 — 별도의 순서 변경(reorder) 엔드포인트가 명세서에 없기 때문이다. 그래서 삭제 시 뒤 순서를
 * 당기는 등의 자동 재정렬은 하지 않는다.
 */
@Entity
@Table(name = "curriculum")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Curriculum extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generationId;

  /** order 는 SQL 예약어라 컬럼명을 분리한다. */
  @Column(name = "curriculum_order", nullable = false)
  private Integer order;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 255)
  private String subtitle;

  @Builder(access = AccessLevel.PRIVATE)
  private Curriculum(Long generationId, Integer order, String title, String subtitle) {
    this.generationId = generationId;
    this.order = order;
    this.title = title;
    this.subtitle = subtitle;
  }

  public static Curriculum create(Long generationId, Integer order, String title, String subtitle) {
    return Curriculum.builder()
        .generationId(generationId)
        .order(order)
        .title(title)
        .subtitle(subtitle)
        .build();
  }

  /** 10.12. generationId 도 함께 바꿀 수 있다 — 명세서 PUT 요청 바디에 generationId 가 포함된다. */
  public void update(Long generationId, Integer order, String title, String subtitle) {
    this.generationId = generationId;
    this.order = order;
    this.title = title;
    this.subtitle = subtitle;
  }
}
