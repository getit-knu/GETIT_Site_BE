package com.getit.domain.setting.curriculum.service;

import java.util.List;

/**
 * 다른 도메인이 커리큘럼을 조회할 때 거치는 계약. (작업 분할 계획 4.2, 홈 통합 조회 2.1 소비 목적)
 *
 * <p>{@code CurriculumRepository} 직접 참조를 대체한다. 존재하지 않는 기수를 넘기면 빈 리스트를
 * 반환한다 — 홈은 공개 API 라 활성 기수가 없어도 404 로 실패하면 안 되기 때문이다.
 */
public interface CurriculumQueryService {

  List<CurriculumView> findByGenerationId(Long generationId);
}
