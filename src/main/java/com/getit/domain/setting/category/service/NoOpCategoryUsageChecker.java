package com.getit.domain.setting.category.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** lecture 도메인(#25) 구현 전까지의 임시 구현체. lecture 생기면 교체한다. */
@Component
public class NoOpCategoryUsageChecker implements CategoryUsageChecker {

  @Override
  public long countLecturesByTrackId(Long trackId) { return 0; }

  @Override
  public long countLecturesBySubCategoryId(Long subCategoryId) { return 0; }

  @Override
  public Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds) {
    return subCategoryIds.stream().collect(Collectors.toMap(Function.identity(), id -> 0L));
  }

  @Override
  public void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds) { }
}
