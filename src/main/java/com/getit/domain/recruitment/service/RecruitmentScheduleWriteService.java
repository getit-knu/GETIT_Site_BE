package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.RecruitmentScheduleResult;
import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;

/**
 * 다른 도메인이 모집 일정을 갱신할 때 거치는 계약. (작업 분할 계획 4.2, 홈 일괄 저장 10.20 소비 목적)
 *
 * <p>{@code RecruitmentScheduleService.updateSchedule}는 활성 기수를 스스로 조회해 대상을
 * 정하고 6.2 검증 규칙을 그대로 적용한다 — 10.20 도 같은 규칙이 적용돼야 하므로 로직을 다시
 * 만들지 않고 그대로 노출한다.
 */
public interface RecruitmentScheduleWriteService {

  RecruitmentScheduleResult updateSchedule(ScheduleUpdateCommand command);
}
