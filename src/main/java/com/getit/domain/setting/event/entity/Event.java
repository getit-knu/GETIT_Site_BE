package com.getit.domain.setting.event.entity;

import com.getit.domain.setting.event.dto.EventCommand;
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
@Table(name = "`event`")
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
  private boolean isVisible;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private EventType type;

  @Column(nullable = false)
  private long generationId;

  @Builder(access = AccessLevel.PRIVATE)
  private Event(EventCommand command, long generationId) {
    this.title = command.title();
    this.place = command.place();
    this.startDate = command.startDate();
    this.endDate = command.endDate();
    this.isVisible = command.isVisible();
    this.type = command.type();
    this.generationId = generationId;
  }

  public static Event create(EventCommand command, long generationId) {
    return Event.builder()
        .command(command)
        .generationId(generationId)
        .build();
  }

  public void update(EventCommand command) {
    this.title = command.title();
    this.place = command.place();
    this.startDate = command.startDate();
    this.endDate = command.endDate();
    this.isVisible = command.isVisible();
    this.type = command.type();
  }
}
