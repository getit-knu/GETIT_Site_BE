package com.getit.domain.lecture.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  @Convert(converter = SubmissionTypeSetConverter.class)
  @Column(name = "allowed_types", nullable = false, length = 100)
  private Set<SubmissionType> allowedTypes;

  @Column(name = "link_placeholder", length = 255)
  private String linkPlaceholder;

  @Builder(access = AccessLevel.PRIVATE)
  private Assignment(
      Long lectureId,
      String title,
      String description,
      LocalDateTime deadline,
      Set<SubmissionType> allowedTypes,
      String linkPlaceholder
  ) {
    this.lectureId = lectureId;
    this.title = title;
    this.description = description;
    this.deadline = deadline;
    this.allowedTypes = allowedTypes;
    this.linkPlaceholder = linkPlaceholder;
  }

  public static Assignment create(
      Long lectureId,
      String title,
      String description,
      LocalDateTime deadline,
      Set<SubmissionType> allowedTypes,
      String linkPlaceholder
  ) {
    return Assignment.builder()
        .lectureId(lectureId)
        .title(title)
        .description(description)
        .deadline(deadline)
        .allowedTypes(allowedTypes)
        .linkPlaceholder(linkPlaceholder)
        .build();
  }

  public void update(
      String title,
      String description,
      LocalDateTime deadline,
      Set<SubmissionType> allowedTypes,
      String linkPlaceholder
  ) {
    this.title = title;
    this.description = description;
    this.deadline = deadline;
    this.allowedTypes = allowedTypes;
    this.linkPlaceholder = linkPlaceholder;
  }
}
