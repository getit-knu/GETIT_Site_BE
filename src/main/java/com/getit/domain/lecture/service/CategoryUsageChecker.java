package com.getit.domain.lecture.service;

import java.util.List;
import java.util.Map;

/**
 * category 도메인이 트랙·소분류 삭제 시 강의 사용 여부를 확인할 때 거치는 계약.
 * (이슈 #25, PR #31 리뷰)
 *
 * <p>{@code LectureRepository} 직접 참조를 대체한다. 데이터 주인(강의)이 lecture 도메인이라
 * {@code category} 가 아닌 이 패키지에 둔다({@code GenerationQueryService} 패턴).
 */
public interface CategoryUsageChecker {

  long countLecturesByTrackId(Long trackId);

  long countLecturesBySubCategoryId(Long subCategoryId);

  Map<Long, Long> countLecturesBySubCategoryIds(List<Long> subCategoryIds);

  void disconnectLecturesBySubCategoryIds(List<Long> subCategoryIds);
}
