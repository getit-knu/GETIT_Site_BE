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

  @Test
  @DisplayName("공개로 생성하면 publishedAt 이 채워지고, 비공개면 null 이다")
  void setsPublishedAtOnlyWhenPublished() {
    Lecture published = Lecture.create(1, "공개", null, null, null, null, true, 9L, 1L, null, 100L);
    Lecture draft = Lecture.create(1, "비공개", null, null, null, null, false, 9L, 1L, null, 100L);

    assertThat(published.getPublishedAt()).isNotNull();
    assertThat(draft.getPublishedAt()).isNull();
  }

  @Test
  @DisplayName("비공개에서 공개로 전환할 때 publishedAt 이 채워진다")
  void setsPublishedAtOnTransitionToPublished() {
    Lecture lecture = Lecture.create(1, "비공개", null, null, null, null, false, 9L, 1L, null, 100L);

    lecture.update(1, "공개됨", null, null, null, null, true, 9L, 1L, null);

    assertThat(lecture.getPublishedAt()).isNotNull();
  }

  @Test
  @DisplayName("이미 공개된 강의를 수정해도 publishedAt 은 최초 공개 시각을 유지한다")
  void keepsPublishedAtWhenAlreadyPublished() {
    Lecture lecture = Lecture.create(1, "공개", null, null, null, null, true, 9L, 1L, null, 100L);
    var firstPublishedAt = lecture.getPublishedAt();

    lecture.update(2, "수정됨", null, null, null, null, true, 9L, 1L, null);

    assertThat(lecture.getPublishedAt()).isEqualTo(firstPublishedAt);
  }
}
