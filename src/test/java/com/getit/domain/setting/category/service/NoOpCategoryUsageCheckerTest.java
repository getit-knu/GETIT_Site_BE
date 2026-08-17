package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.Map;
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

  @Test
  @DisplayName("배치 조회: id마다 0 반환")
  void batchAlwaysReturnsZeroForEachId() {
    Map<Long, Long> result = checker.countLecturesBySubCategoryIds(List.of(1L, 2L));

    assertThat(result).isEqualTo(Map.of(1L, 0L, 2L, 0L));
  }

  @Test
  @DisplayName("연결 해제: lecture 도메인이 없는 동안은 아무 동작도 하지 않는다")
  void disconnectDoesNothing() {
    assertThatCode(() -> checker.disconnectLecturesBySubCategoryIds(List.of(1L, 2L)))
        .doesNotThrowAnyException();
  }
}
