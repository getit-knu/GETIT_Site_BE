package com.getit.domain.project.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 프로젝트에 붙은 이미지 한 장. {@code LectureFile} 처럼 id 만 두고 연관 매핑은 두지 않는다. */
@Entity
@Table(name = "project_image")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectImage extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "file_id", nullable = false)
  private Long fileId;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private ProjectImageKind kind;

  @Column(name = "sort_order", nullable = false)
  private int sortOrder;

  public static ProjectImage of(Long projectId, Long fileId, ProjectImageKind kind, int sortOrder) {
    ProjectImage image = new ProjectImage();
    image.projectId = projectId;
    image.fileId = fileId;
    image.kind = kind;
    image.sortOrder = sortOrder;
    return image;
  }

  public void moveTo(int sortOrder) {
    this.sortOrder = sortOrder;
  }
}
