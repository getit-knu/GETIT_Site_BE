package com.getit.domain.lecture.service;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 도메인이 강의 제목을 조회할 때 거치는 계약. qna(11.1 · 11.2 · D5.2)의 lectureTitle 용.
 */
public interface LectureQueryService {

  Map<Long, String> findTitlesByIds(Collection<Long> lectureIds);
}
