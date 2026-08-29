package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.RecruitmentScheduleResult;
import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;
import com.getit.domain.setting.generation.dto.GenerationSummary;

/**
 * 다른 도메인이 모집 일정을 갱신할 때 거치는 계약. (작업 분할 계획 4.2, 홈 일괄 저장 10.20 소비 목적)
 *
 * <p>{@code RecruitmentScheduleService.updateSchedule}는 활성 기수를 스스로 조회해 대상을
 * 정하고 6.2 검증 규칙을 그대로 적용한다 — 10.20 도 같은 규칙이 적용돼야 하므로 로직을 다시
 * 만들지 않고 그대로 노출한다.
 */
public interface RecruitmentScheduleWriteService {

  RecruitmentScheduleResult updateSchedule(ScheduleUpdateCommand command);

  /**
   * 호출자가 이미 활성 기수를 조회해둔 경우 쓴다 — {@code updateSchedule(command)}가 내부에서
   * 다시 활성 기수를 조회하면, 홈 일괄 저장(10.20)처럼 여러 계약을 한 트랜잭션 안에서 순차
   * 호출하는 도중 활성 기수가 바뀌었을 때(동시 admin 작업) 일정은 새 활성 기수에, 나머지는
   * 먼저 확보해둔 기존 활성 기수에 반영되는 상황이 생길 수 있다(PR #136 Copilot 리뷰 지적).
   */
  RecruitmentScheduleResult updateSchedule(GenerationSummary activeGeneration, ScheduleUpdateCommand command);
}
