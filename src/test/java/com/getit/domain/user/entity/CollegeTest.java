package com.getit.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CollegeTest {

  @Test
  @DisplayName("단과대학을 생성한다")
  void createsCollege() {
    College college = College.create("경영대학");

    assertThat(college.getName()).isEqualTo("경영대학");
  }
}
