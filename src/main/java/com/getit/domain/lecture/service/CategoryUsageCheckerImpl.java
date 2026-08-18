package com.getit.domain.lecture.service;

import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.lecture.repository.LectureRepository.SubCategoryLectureCount;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryUsageCheckerImpl implements CategoryUsageChecker {

  private final LectureRepository lectureRepository;

  @Override
  public long countLecturesByTrackId(Long trackId) {
    return lectureRepository.countByTrackIdAndDeletedAtIsNull(trackId);
  }

  @Override
  public long countLecturesBySubCategoryId(Long subCategoryId) {
    return lectureRepository.countBySubCategoryIdAndDeletedAtIsNull(subCategoryId);
  }

  @Override
  public Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds) {
    Map<Long, Long> counts = lectureRepository.countBySubCategoryIdsGrouped(subCategoryIds).stream()
        .collect(Collectors.toMap(SubCategoryLectureCount::getSubCategoryId, SubCategoryLectureCount::getCount));

    return subCategoryIds.stream()
        .collect(Collectors.toMap(Function.identity(), id -> counts.getOrDefault(id, 0L)));
  }

  @Override
  @Transactional
  public void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds) {
    lectureRepository.disconnectBySubCategoryIds(subCategoryIds);
  }

  @Override
  @Transactional
  public void disconnectLecturesByTrackId(Long trackId) {
    lectureRepository.disconnectByTrackId(trackId);
  }
}
