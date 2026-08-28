package com.getit.domain.setting.curriculum.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CurriculumTest {

  @Test
  @DisplayName("생성한다")
  void creates() {
    Curriculum curriculum = Curriculum.create(9L, 1, "Python & 데이터 분석", "Python 기초부터 데이터 분석까지");

    assertThat(curriculum.getGenerationId()).isEqualTo(9L);
    assertThat(curriculum.getOrder()).isEqualTo(1);
    assertThat(curriculum.getTitle()).isEqualTo("Python & 데이터 분석");
    assertThat(curriculum.getSubtitle()).isEqualTo("Python 기초부터 데이터 분석까지");
  }

  @Test
  @DisplayName("수정한다")
  void updates() {
    Curriculum curriculum = Curriculum.create(9L, 1, "Python & 데이터 분석", "Python 기초부터 데이터 분석까지");

    curriculum.update(9L, "웹 개발", "React, Node.js를 활용한 웹 서비스 개발");

    assertThat(curriculum.getTitle()).isEqualTo("웹 개발");
    assertThat(curriculum.getSubtitle()).isEqualTo("React, Node.js를 활용한 웹 서비스 개발");
    assertThat(curriculum.getOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("순서를 변경한다")
  void updatesOrder() {
    Curriculum curriculum = Curriculum.create(9L, 1, "Python & 데이터 분석", "Python 기초부터 데이터 분석까지");

    curriculum.updateOrder(3);

    assertThat(curriculum.getOrder()).isEqualTo(3);
  }
}
