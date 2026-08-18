package com.getit.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MajorTest {

  @Test
  @DisplayName("전공을 생성한다")
  void createsMajor() {
    Major major = Major.create(1L, "경영학과");

    assertThat(major.getCollegeId()).isEqualTo(1L);
    assertThat(major.getName()).isEqualTo("경영학과");
  }
}
