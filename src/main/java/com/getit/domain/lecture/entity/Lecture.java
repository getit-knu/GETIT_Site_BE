package com.getit.domain.lecture.entity;

import com.getit.global.entity.SoftDeletableEntity;
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

@Entity
@Table(name = "lecture")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Lecture extends SoftDeletableEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer week;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Column(name = "youtube_url", length = 512)
  private String youtubeUrl;

  @Column(name = "material_url", length = 512)
  private String materialUrl;

  @Column(name = "duration_minutes")
  private Integer durationMinutes;

  @Column(name = "is_published", nullable = false)
  private boolean published;

  @Column(name = "generation_id", nullable = false)
  private Long generationId;

  @Column(name = "track_id", nullable = false)
  private Long trackId;

  @Column(name = "sub_category_id")
  private Long subCategoryId;

  @Column(name = "created_by", nullable = false)
  private Long createdBy;

  @Builder(access = AccessLevel.PRIVATE)
  private Lecture(
      Integer week,
      String title,
      String description,
      String youtubeUrl,
      String materialUrl,
      Integer durationMinutes,
      boolean published,
      Long generationId,
      Long trackId,
      Long subCategoryId,
      Long createdBy
  ) {
    this.week = week;
    this.title = title;
    this.description = description;
    this.youtubeUrl = youtubeUrl;
    this.materialUrl = materialUrl;
    this.durationMinutes = durationMinutes;
    this.published = published;
    this.generationId = generationId;
    this.trackId = trackId;
    this.subCategoryId = subCategoryId;
    this.createdBy = createdBy;
  }

  public static Lecture create(
      Integer week,
      String title,
      String description,
      String youtubeUrl,
      String materialUrl,
      Integer durationMinutes,
      boolean published,
      Long generationId,
      Long trackId,
      Long subCategoryId,
      Long createdBy
  ) {
    return Lecture.builder()
        .week(week)
        .title(title)
        .description(description)
        .youtubeUrl(youtubeUrl)
        .materialUrl(materialUrl)
        .durationMinutes(durationMinutes)
        .published(published)
        .generationId(generationId)
        .trackId(trackId)
        .subCategoryId(subCategoryId)
        .createdBy(createdBy)
        .build();
  }

  public boolean isPublished() { return published; }
}
