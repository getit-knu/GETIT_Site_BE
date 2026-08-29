package com.getit.domain.dashboard.service;

import com.getit.domain.dashboard.dto.SubmissionStatusResult;
import com.getit.domain.lecture.service.LectureStatService;
import com.getit.domain.lecture.service.WeeklySubmissionStat;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.service.UserQueryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주차별 과제 제출 현황. (API 명세서 5.3)
 *
 * <p>활성 기수가 없으면 {@code totalMemberCount} 0, {@code weeks} 빈 리스트로 응답한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionStatusService {

  private final GenerationQueryService generationQueryService;
  private final UserQueryService userQueryService;
  private final LectureStatService lectureStatService;

  public SubmissionStatusResult getSubmissionStatus(Long trackId, int size) {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();
    if (activeGeneration.isEmpty()) {
      return new SubmissionStatusResult(0L, List.of());
    }

    GenerationSummary generation = activeGeneration.get();
    long totalMemberCount = userQueryService.countActiveMembersInGeneration(generation.generationNo());
    List<WeeklySubmissionStat> stats = lectureStatService.findWeeklyStats(generation.generationNo(), trackId, size);

    List<SubmissionStatusResult.WeekStat> weeks = stats.stream()
        .map(stat -> toWeekStat(stat, totalMemberCount))
        .toList();

    return new SubmissionStatusResult(totalMemberCount, weeks);
  }

  private SubmissionStatusResult.WeekStat toWeekStat(WeeklySubmissionStat stat, long totalMemberCount) {
    double rate = totalMemberCount == 0 ? 0.0 : round1((stat.submittedCount() * 100.0) / totalMemberCount);
    return new SubmissionStatusResult.WeekStat(
        stat.lectureId(), stat.week(), stat.title(), stat.submittedCount(), totalMemberCount, rate);
  }

  private double round1(double value) {
    return Math.round(value * 10) / 10.0;
  }
}
