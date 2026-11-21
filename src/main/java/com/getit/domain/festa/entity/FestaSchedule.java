package com.getit.domain.festa.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FESTA 투표 기간. 행이 하나뿐이고(id 고정), 행이 없으면 "미설정"이다. (이슈 #234)
 *
 * <p>시각은 Asia/Seoul 기준 LocalDateTime 으로 저장한다 (프로젝트 공통 규칙).
 */
@Entity
@Table(name = "festa_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaSchedule extends BaseTimeEntity {

  public static final long SINGLETON_ID = 1L;

  @Id
  private Long id;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  public static FestaSchedule of(LocalDateTime startAt, LocalDateTime endAt) {
    FestaSchedule schedule = new FestaSchedule();
    schedule.id = SINGLETON_ID;
    schedule.startAt = startAt;
    schedule.endAt = endAt;
    return schedule;
  }

  public void change(LocalDateTime startAt, LocalDateTime endAt) {
    this.startAt = startAt;
    this.endAt = endAt;
  }

  /** startAt <= now < endAt */
  public boolean contains(LocalDateTime now) {
    return !now.isBefore(startAt) && now.isBefore(endAt);
  }
}
