package com.getit.domain.festa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서버가 뽑은 경품 당첨자. (이슈 #238)
 *
 * <p>번호는 여기 복사하지 않는다 — {@link FestaInvestor} 에만 두어 파기하면 당첨자 번호도 함께 사라진다.
 * 추첨 결과를 덮어쓰지 않도록 수정 메서드를 두지 않는다. 추가 추첨은 순번을 이어 붙인다.
 */
@Entity
@Table(name = "festa_winner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaWinner {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  /** 1부터. 추가 추첨이면 앞 결과 다음 번호부터 매긴다. */
  @Column(name = "draw_order", nullable = false)
  private int drawOrder;

  @Column(name = "drawn_by", nullable = false)
  private Long drawnBy;

  @Column(name = "drawn_at", nullable = false)
  private LocalDateTime drawnAt;

  public static FestaWinner of(Long userId, int drawOrder, Long drawnBy, LocalDateTime drawnAt) {
    FestaWinner winner = new FestaWinner();
    winner.userId = userId;
    winner.drawOrder = drawOrder;
    winner.drawnBy = drawnBy;
    winner.drawnAt = drawnAt;
    return winner;
  }
}
