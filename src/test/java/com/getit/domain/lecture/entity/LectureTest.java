package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LectureTest {

  @Test
  @DisplayName("필수값과 선택값으로 생성된다")
  void createsLecture() {
    Lecture lecture = Lecture.create(
        1, "HTML/CSS 기초", "## 학습 구성", "https://youtube.com/watch?v=abc123",
        "https://docs.getit.com/web-basic", 120, true, 9L, 1L, 1L, 100L);

    assertThat(lecture.getWeek()).isEqualTo(1);
    assertThat(lecture.getTitle()).isEqualTo("HTML/CSS 기초");
    assertThat(lecture.getGenerationId()).isEqualTo(9L);
    assertThat(lecture.getTrackId()).isEqualTo(1L);
    assertThat(lecture.getSubCategoryId()).isEqualTo(1L);
    assertThat(lecture.getCreatedBy()).isEqualTo(100L);
    assertThat(lecture.isPublished()).isTrue();
  }

  @Test
  @DisplayName("소분류 없이도 생성된다")
  void createsWithoutSubCategory() {
    Lecture lecture = Lecture.create(
        1, "OT", null, null, null, null, false, 9L, 1L, null, 100L);

    assertThat(lecture.getSubCategoryId()).isNull();
  }
}
