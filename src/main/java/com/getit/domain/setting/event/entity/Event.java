package com.getit.domain.setting.event.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(nullable = false, length = 100)
  private String place;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column(nullable = false)
  private LocalDate endDate;

  @Column(nullable = false)
  private boolean visible;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private EventType type;

  @Column(nullable = false)
  private long generationId;

  @Builder(access = AccessLevel.PRIVATE)
  private Event(
      String title,
      String place,
      LocalDate startDate,
      LocalDate endDate,
      boolean visible,
      EventType type,
      long generationId
  ) {
    this.title = title;
    this.place = place;
    this.startDate = startDate;
    this.endDate = endDate;
    this.visible = visible;
    this.type = type;
    this.generationId = generationId;
  }

  public static Event create(
      String title,
      String place,
      LocalDate startDate,
      LocalDate endDate,
      boolean visible,
      EventType type,
      long generationId
  ) {
    return Event.builder()
        .title(title)
        .place(place)
        .startDate(startDate)
        .endDate(endDate)
        .visible(visible)
        .type(type)
        .generationId(generationId)
        .build();
  }

  public void update(
      String title,
      String place,
      LocalDate startDate,
      LocalDate endDate,
      boolean visible,
      EventType type,
      long generationId
  ) {
    this.title = title;
    this.place = place;
    this.startDate = startDate;
    this.endDate = endDate;
    this.visible = visible;
    this.type = type;
    this.generationId = generationId;
  }
}
