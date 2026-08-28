package com.getit.domain.lecture.service;

import java.util.List;

/**
 * D5 대시보드(A 소유)가 소비하는 lecture 통계 조회 계약. 크로스도메인이라 interface + Impl 로 분리한다.
 * 순수 lecture 데이터만 반환한다 — 회원 수 · 카테고리 이름 등은 A 가 각 도메인 계약으로 채운다.
 * D5.3 의 trackId 필터는 A 가 대시보드를 착수할 때 계약을 확장한다.
 */
public interface LectureStatService {

  long countUnEvaluatedSubmissions(int generationNo);

  List<WeeklySubmissionStat> findWeeklyStats(int generationNo, int size);

  List<OngoingLectureStat> findOngoingLectures(int generationNo);
}
