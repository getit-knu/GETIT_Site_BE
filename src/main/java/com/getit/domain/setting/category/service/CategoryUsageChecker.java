package com.getit.domain.setting.category.service;

public interface CategoryUsageChecker {

  long countLecturesByTrackId(Long trackId);

  long countLecturesBySubCategoryId(Long subCategoryId);
}
