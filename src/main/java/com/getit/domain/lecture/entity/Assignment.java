package com.getit.domain.lecture.entity;

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

/** 강의별 과제. 강의당 최대 1개(선택)라 {@code lectureId} 에 unique 제약을 둔다. */
@Entity
@Table(name = "assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assignment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "lecture_id", nullable = false, unique = true)
  private Long lectureId;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(nullable = false)
  private LocalDateTime deadline;

  @Builder(access = AccessLevel.PRIVATE)
  private Assignment(Long lectureId, String title, String description, LocalDateTime deadline) {
    this.lectureId = lectureId;
    this.title = title;
    this.description = description;
    this.deadline = deadline;
  }

  public static Assignment create(Long lectureId, String title, String description, LocalDateTime deadline) {
    return Assignment.builder()
        .lectureId(lectureId)
        .title(title)
        .description(description)
        .deadline(deadline)
        .build();
  }
}
