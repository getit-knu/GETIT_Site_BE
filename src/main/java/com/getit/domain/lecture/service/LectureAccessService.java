package com.getit.domain.lecture.service;

/**
 * 다른 도메인이 "이 부원이 이 강의를 볼 수 있는가" 를 확인할 때 거치는 계약. qna(4.6 · 4.7)가 쓴다.
 * 4.1~4.5 와 동일한 가드다.
 */
public interface LectureAccessService {

  /** 비활성 부원(role · 활성 기수 불일치) → 403, 비공개 · 타기수 · 삭제 강의 → 404. */
  void requireVisibleToMember(Long lectureId, Long userId);
}
