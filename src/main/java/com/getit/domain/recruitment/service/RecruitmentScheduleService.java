package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.RecruitmentScheduleResult;
import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 모집 일정 조회 · 설정. (API 명세서 6.1 · 6.2) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentScheduleService implements RecruitmentScheduleWriteService {

  private final RecruitmentScheduleRepository recruitmentScheduleRepository;
  private final GenerationQueryService generationQueryService;

  public RecruitmentScheduleResult getSchedule() {
    GenerationSummary activeGeneration = findActiveGeneration();

    RecruitmentSchedule schedule = recruitmentScheduleRepository.findByGenerationId(activeGeneration.id())
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.SCHEDULE_NOT_FOUND));

    return RecruitmentScheduleResult.of(activeGeneration, schedule);
  }

  /**
   * 지원 접수를 여닫는다. (이슈 #170)
   *
   * <p>일정은 건드리지 않는다. 급히 멈추려고 {@code documentEndAt} 을 과거로 당기면 원래
   * 마감 일정이 지워지고 공개 화면의 D-day 와 일정 표시까지 함께 망가진다.
   */
  @Transactional
  public RecruitmentScheduleResult changeApplyEnabled(boolean enabled) {
    GenerationSummary activeGeneration = findActiveGeneration();
    RecruitmentSchedule schedule = recruitmentScheduleRepository.findByGenerationId(activeGeneration.id())
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.SCHEDULE_NOT_FOUND));

    schedule.changeApplyEnabled(enabled);
    return RecruitmentScheduleResult.of(activeGeneration, schedule);
  }

  @Override
  @Transactional
  public RecruitmentScheduleResult updateSchedule(ScheduleUpdateCommand command) {
    return updateSchedule(findActiveGeneration(), command);
  }

  @Override
  @Transactional
  public RecruitmentScheduleResult updateSchedule(GenerationSummary activeGeneration, ScheduleUpdateCommand command) {
    LocalDateTime totalStartAt = command.totalStartAt();
    LocalDateTime totalEndAt = command.totalEndAt();
    LocalDateTime documentStartAt = command.documentStartAt();
    LocalDateTime documentEndAt = command.documentEndAt();
    LocalDateTime interviewStartAt = command.interviewStartAt();
    validateOrder(totalStartAt, totalEndAt, documentStartAt, documentEndAt, interviewStartAt);

    RecruitmentSchedule schedule = recruitmentScheduleRepository.findByGenerationId(activeGeneration.id())
        .map(existing -> {
          existing.update(totalStartAt, totalEndAt, documentStartAt, documentEndAt, interviewStartAt);
          return existing;
        })
        .orElseGet(() -> recruitmentScheduleRepository.save(
            RecruitmentSchedule.create(
                activeGeneration.id(),
                totalStartAt, totalEndAt, documentStartAt, documentEndAt, interviewStartAt)));

    return RecruitmentScheduleResult.of(activeGeneration, schedule);
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  /** API 명세서 6.2 검증 규칙. */
  private void validateOrder(
      LocalDateTime totalStartAt,
      LocalDateTime totalEndAt,
      LocalDateTime documentStartAt,
      LocalDateTime documentEndAt,
      LocalDateTime interviewStartAt
  ) {
    if (!totalStartAt.isBefore(totalEndAt)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "총 모집 시작일은 종료일보다 빨라야 합니다.");
    }
    if (!documentStartAt.isBefore(documentEndAt) || documentEndAt.isAfter(totalEndAt)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "서류 기간은 시작일이 종료일보다 빠르고 총 모집 기간 안에 있어야 합니다.");
    }
    if (documentEndAt.isAfter(interviewStartAt)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "면접 시작일은 서류 마감일 이후여야 합니다.");
    }
    // interviewEndAt 은 totalEndAt 으로 강제 동기화된다. 이 검증이 없으면
    // interviewStartAt 이 totalEndAt 보다 늦어 interviewEndAt < interviewStartAt 인
    // 깨진 일정이 저장된다.
    if (interviewStartAt.isAfter(totalEndAt)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "면접 시작일은 총 모집 종료일보다 늦을 수 없습니다.");
    }
  }
}
