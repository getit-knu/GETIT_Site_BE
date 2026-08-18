package com.getit.domain.setting.category.service;

import java.util.List;
import java.util.Map;

public interface CategoryUsageChecker {

  long countLecturesByTrackId(Long trackId);

  long countLecturesBySubCategoryId(Long subCategoryId);

  Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds);
}
