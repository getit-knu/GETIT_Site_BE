package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.RecruitmentStatusResult;

/**
 * 다른 도메인이 모집 상태 · D-day 를 조회할 때 거치는 계약. (작업 분할 계획 4.2, 홈 통합 조회 2.1 소비 목적)
 *
 * <p>2.1(홈)과 2.8(공개 모집 상태)이 phase · D-day · 안내 메시지 계산 로직을 그대로 공유해야 해서,
 * 계산 로직을 다시 만들지 않고 이미 있는 {@code RecruitmentStatusService.getStatus()} 결과를
 * 인터페이스로 노출한다({@code RecruitmentStatusResult} 는 이미 오프셋 변환까지 끝난 응답 전용
 * 값이라, {@code FaqView} 류의 raw View 와 달리 계산 결과 자체를 그대로 재사용한다).
 */
public interface RecruitmentStatusQueryService {

  RecruitmentStatusResult getStatus();
}
