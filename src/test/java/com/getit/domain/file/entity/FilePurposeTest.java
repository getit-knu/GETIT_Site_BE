package com.getit.domain.file.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FilePurposeTest {

  @Test
  @DisplayName("허용된 확장자: true")
  void allowsListedExtension() {
    assertThat(FilePurpose.ASSIGNMENT.allows("pdf")).isTrue();
  }

  @Test
  @DisplayName("허용 안 된 확장자: false")
  void rejectsUnlistedExtension() {
    assertThat(FilePurpose.ASSIGNMENT.allows("exe")).isFalse();
  }

  @Test
  @DisplayName("대소문자: 구분 안 함")
  void isCaseInsensitive() {
    assertThat(FilePurpose.PROFILE_IMAGE.allows("PNG")).isTrue();
    assertThat(FilePurpose.PROFILE_IMAGE.allows("Png")).isTrue();
  }

  @Test
  @DisplayName("PROFILE_IMAGE,PROJECT_THUMBNAIL: 5MB 제한")
  void smallPurposesHave5MbLimit() {
    assertThat(FilePurpose.PROFILE_IMAGE.getMaxSizeBytes()).isEqualTo(5 * 1024 * 1024L);
    assertThat(FilePurpose.PROJECT_THUMBNAIL.getMaxSizeBytes()).isEqualTo(5 * 1024 * 1024L);
    assertThat(FilePurpose.PROJECT_IMAGE.getMaxSizeBytes()).isEqualTo(5 * 1024 * 1024L);
  }

  @Test
  @DisplayName("LECTURE_MATERIAL,ASSIGNMENT: 50MB 제한")
  void largePurposesHave50MbLimit() {
    assertThat(FilePurpose.LECTURE_MATERIAL.getMaxSizeBytes()).isEqualTo(50 * 1024 * 1024L);
    assertThat(FilePurpose.ASSIGNMENT.getMaxSizeBytes()).isEqualTo(50 * 1024 * 1024L);
  }
}
