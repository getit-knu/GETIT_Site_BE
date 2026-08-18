package com.getit.domain.lecture.service;

import java.util.List;
import java.util.Map;

/**
 * category 도메인이 트랙·소분류 삭제 시 강의 사용 여부를 확인하고
 * 연결을 해제할 때 거치는 계약.
 */
public interface CategoryUsageChecker {

  long countLecturesByTrackId(Long trackId);

  long countLecturesBySubCategoryId(Long subCategoryId);

  Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesByTrackId(Long trackId);
}
