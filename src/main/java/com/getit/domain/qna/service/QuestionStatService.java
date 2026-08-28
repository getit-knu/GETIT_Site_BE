package com.getit.domain.qna.service;

import java.util.List;

/**
 * D5 대시보드(A 소유)가 소비하는 Q&A 통계 조회 계약. 순수 qna 데이터만 반환한다 —
 * authorName · lectureTitle · elapsedLabel 은 A 가 각 도메인 계약으로 채운다.
 */
public interface QuestionStatService {

  long countUnanswered();

  List<RecentQuestion> findRecent(int size);
}
