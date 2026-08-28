package com.getit.domain.setting.faq.entity;

import com.getit.domain.setting.faq.dto.FaqCommand;
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
 * FAQ 항목. 공개 사이트 FAQ 목록(2.5)에 노출되는 문답 하나를 나타낸다. (API 명세서 10.18 ~ 10.19)
 *
 * <p>{@code order} 는 {@code Curriculum} 과 동일하게 별도 순서 변경 엔드포인트가 없어 생성 · 수정
 * 요청 바디로 직접 다룬다. 1..N 연속 불변식은 {@code FaqAdminService} 가 유지한다.
 */
@Entity
@Table(name = "faq")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** order 는 SQL 예약어라 컬럼명을 분리한다. */
  @Column(name = "faq_order", nullable = false)
  private int order;

  @Column(nullable = false, length = 255)
  private String question;

  @Column(nullable = false, length = 2000)
  private String answer;

  @Column(nullable = false)
  private boolean isVisible;

  @Builder(access = AccessLevel.PRIVATE)
  private Faq(FaqCommand command, int order) {
    this.order = order;
    this.question = command.question();
    this.answer = command.answer();
    this.isVisible = command.isVisible();
  }

  public static Faq create(FaqCommand command, int order) {
    return Faq.builder()
        .command(command)
        .order(order)
        .build();
  }

  public void update(FaqCommand command) {
    this.question = command.question();
    this.answer = command.answer();
    this.isVisible = command.isVisible();
  }

  /** 생성·수정·삭제로 다른 항목의 순서가 밀리거나 당겨질 때 쓴다. */
  public void updateOrder(int order) {
    this.order = order;
  }
}
