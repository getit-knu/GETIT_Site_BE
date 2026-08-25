package com.getit.domain.lecture.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
  name = "lecture_file",
  uniqueConstraints = {
    @UniqueConstraint(name = "uk_lecture_file_lecture_id_file_id", columnNames = {"lecture_id", "file_id"})
  }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LectureFile extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "display_name", nullable = false, length = 255)
  private String displayName;

  @Column(name = "lecture_id", nullable = false)
  private Long lectureId;

  @Column(name = "file_id", nullable = false)
  private Long fileId;

  @Builder(access = AccessLevel.PRIVATE)
  private LectureFile(String displayName, Long lectureId, Long fileId) {
    this.displayName = displayName;
    this.lectureId = lectureId;
    this.fileId = fileId;
  }

  public static LectureFile create(String displayName, Long lectureId, Long fileId) {
    return LectureFile.builder()
        .displayName(displayName)
        .lectureId(lectureId)
        .fileId(fileId)
        .build();
  }
}
