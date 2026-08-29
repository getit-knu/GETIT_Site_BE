package com.getit.domain.dashboard.service;

import com.getit.domain.dashboard.dto.DashboardSummaryResult;
import com.getit.domain.lecture.service.LectureStatService;
import com.getit.domain.qna.service.QuestionStatService;
import com.getit.domain.recruitment.service.ApplicationQueryService;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.service.UserQueryService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상단 카운터 4종. (API 명세서 5.1)
 *
 * <p>{@code memberCount}·{@code unansweredQuestionCount}는 기수와 무관한 전체 카운트다(명세서
 * 산출 기준 표에 기수 조건이 없다). {@code totalApplicants}·{@code unEvaluatedAssignmentCount}는
 * 활성 기수 기준이라, 활성 기수가 없으면 0으로 응답한다(공개 API 는 아니지만 조회 전용 화면이라
 * 관리자 CRUD 처럼 404 로 실패시키지 않는다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardSummaryService {

  private final GenerationQueryService generationQueryService;
  private final ApplicationQueryService applicationQueryService;
  private final UserQueryService userQueryService;
  private final LectureStatService lectureStatService;
  private final QuestionStatService questionStatService;

  public DashboardSummaryResult getSummary() {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();

    long totalApplicants = activeGeneration
        .map(generation -> applicationQueryService.countSubmittedByGenerationId(generation.id()))
        .orElse(0L);
    long unEvaluatedAssignmentCount = activeGeneration
        .map(generation -> lectureStatService.countUnEvaluatedSubmissions(generation.generationNo()))
        .orElse(0L);

    return new DashboardSummaryResult(
        totalApplicants,
        userQueryService.countActiveMembers(),
        unEvaluatedAssignmentCount,
        questionStatService.countUnanswered());
  }
}
