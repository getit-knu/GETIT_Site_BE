package com.getit.domain.lecture.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AssignmentTest {

  @Test
  @DisplayName("제목·내용·마감일로 생성된다")
  void createsAssignment() {
    LocalDateTime deadline = LocalDateTime.of(2026, 6, 19, 23, 59, 59);

    Assignment assignment =
        Assignment.create(1L, "자기소개 페이지 만들기", "HTML과 CSS로 만들어보세요.", deadline);

    assertThat(assignment.getLectureId()).isEqualTo(1L);
    assertThat(assignment.getTitle()).isEqualTo("자기소개 페이지 만들기");
    assertThat(assignment.getDeadline()).isEqualTo(deadline);
  }
}
