package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoOpCategoryUsageCheckerTest {

  private final NoOpCategoryUsageChecker checker = new NoOpCategoryUsageChecker();

  @Test
  @DisplayName("lecture 도메인이 없는 동안은 항상 0을 반환한다")
  void alwaysReturnsZero() {
    assertThat(checker.countLecturesByTrackId(1L)).isZero();
    assertThat(checker.countLecturesBySubCategoryId(1L)).isZero();
  }
}
