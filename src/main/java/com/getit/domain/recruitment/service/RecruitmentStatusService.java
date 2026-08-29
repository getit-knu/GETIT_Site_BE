package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.RecruitmentStatusResult;
import com.getit.domain.recruitment.entity.RecruitmentPhase;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모집 상태 · D-day (공개). (API 명세서 2.8)
 *
 * <p>활성 기수가 없으면(또는 일정이 아직 등록되지 않았으면) {@code CLOSED} 로 응답한다 —
 * 공개 API 라 관리자 API 처럼 404 로 실패하지 않는다({@code RecruitmentSchedule.resolvePhase}
 * 의 CLOSED 설명과 같은 이유).
 *
 * <p>각 단계의 D-day·안내 메시지는 명세서가 {@code BEFORE_OPEN}·{@code DOCUMENT_OPEN} 두
 * 예시만 주고 나머지는 정하지 않아서, 다음 마일스톤까지 남은 일수를 보여주는 방식으로
 * 일관되게 판단해 채웠다(PR 리뷰 포인트).
 *
 * <p>{@code LocalDateTime.now()} 를 직접 부르지 않고 {@link Clock} 을 주입받는다 —
 * 테스트에서 단계 경계값을 고정하지 못하면 테스트가 실행되는 실제 시각에 따라 결과가
 * 달라지고, 자정을 사이에 두면 D-day 검증이 간헐적으로 실패할 수 있다(PR #86 Copilot
 * 리뷰 지적). 서비스 테스트는 고정된 {@code Clock} 으로 교체해서 이 문제를 없앤다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecruitmentStatusService implements RecruitmentStatusQueryService {

  private final RecruitmentScheduleRepository recruitmentScheduleRepository;
  private final GenerationQueryService generationQueryService;
  private final Clock clock;

  @Override
  public RecruitmentStatusResult getStatus() {
    return getStatus(generationQueryService.findActive().orElse(null));
  }

  @Override
  public RecruitmentStatusResult getStatus(GenerationSummary activeGeneration) {
    if (activeGeneration == null) {
      return closedResult();
    }

    Optional<RecruitmentSchedule> schedule = recruitmentScheduleRepository.findByGenerationId(activeGeneration.id());
    if (schedule.isEmpty()) {
      return closedResult();
    }

    return buildResult(activeGeneration, schedule.get());
  }

  private RecruitmentStatusResult buildResult(GenerationSummary generation, RecruitmentSchedule schedule) {
    LocalDateTime now = LocalDateTime.now(clock);
    RecruitmentPhase phase = schedule.resolvePhase(now);
    Long dDay = resolveDDay(phase, schedule, now.toLocalDate());

    return new RecruitmentStatusResult(
        generation.generationNo(),
        generation.year(),
        phase,
        dDay,
        resolveMessage(phase, dDay),
        phase == RecruitmentPhase.DOCUMENT_OPEN,
        RecruitmentStatusResult.ScheduleWindow.from(schedule)
    );
  }

  /** 각 단계가 다음 마일스톤으로 넘어가는 시각까지 남은 일수. 발표 이후에는 의미가 없어 null 이다. */
  private Long resolveDDay(RecruitmentPhase phase, RecruitmentSchedule schedule, LocalDate today) {
    return switch (phase) {
      case BEFORE_OPEN -> ChronoUnit.DAYS.between(today, schedule.getDocumentStartAt().toLocalDate());
      case DOCUMENT_OPEN -> ChronoUnit.DAYS.between(today, schedule.getDocumentEndAt().toLocalDate());
      case DOCUMENT_REVIEW -> ChronoUnit.DAYS.between(today, schedule.getInterviewStartAt().toLocalDate());
      case INTERVIEW -> ChronoUnit.DAYS.between(today, schedule.getTotalEndAt().toLocalDate());
      case FINAL_ANNOUNCED, CLOSED -> null;
    };
  }

  private String resolveMessage(RecruitmentPhase phase, Long dDay) {
    return switch (phase) {
      case BEFORE_OPEN -> "모집 시작까지 D-" + dDay;
      case DOCUMENT_OPEN -> "서류 마감까지 D-" + dDay;
      case DOCUMENT_REVIEW -> "면접까지 D-" + dDay;
      case INTERVIEW -> "최종 발표까지 D-" + dDay;
      case FINAL_ANNOUNCED -> "최종 합격자가 발표되었습니다";
      case CLOSED -> "예정된 모집이 없습니다";
    };
  }

  private RecruitmentStatusResult closedResult() {
    return new RecruitmentStatusResult(
        null, null, RecruitmentPhase.CLOSED, null, "예정된 모집이 없습니다", false, null);
  }
}
