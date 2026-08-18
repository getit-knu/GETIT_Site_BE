package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LectureFileTest {

  @Test
  @DisplayName("표시 이름과 강의·파일 id로 생성된다")
  void createsLectureFile() {
    LectureFile lectureFile = LectureFile.create("1주차 자료.pdf", 1L, 501L);

    assertThat(lectureFile.getDisplayName()).isEqualTo("1주차 자료.pdf");
    assertThat(lectureFile.getLectureId()).isEqualTo(1L);
    assertThat(lectureFile.getFileId()).isEqualTo(501L);
  }
}
