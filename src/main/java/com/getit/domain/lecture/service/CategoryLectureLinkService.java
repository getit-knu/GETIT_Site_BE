package com.getit.domain.lecture.service;

import java.util.List;
import java.util.Map;

public interface CategoryLectureLinkService {

  long countLecturesByTrackId(Long trackId);

  long countLecturesBySubCategoryId(Long subCategoryId);

  Map<Long, Long> countLecturesByTrackIds(List<Long> trackIds);

  Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesByTrackId(Long trackId);
}
