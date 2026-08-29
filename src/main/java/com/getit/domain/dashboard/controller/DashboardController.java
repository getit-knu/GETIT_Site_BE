package com.getit.domain.dashboard.controller;

import com.getit.domain.dashboard.dto.DashboardSummaryResult;
import com.getit.domain.dashboard.dto.OngoingLectureResult;
import com.getit.domain.dashboard.dto.RecentQuestionResult;
import com.getit.domain.dashboard.dto.SubmissionStatusResult;
import com.getit.domain.dashboard.dto.UpcomingEventResult;
import com.getit.domain.dashboard.service.DashboardSummaryService;
import com.getit.domain.dashboard.service.OngoingLectureService;
import com.getit.domain.dashboard.service.RecentQuestionService;
import com.getit.domain.dashboard.service.SubmissionStatusService;
import com.getit.domain.dashboard.service.UpcomingEventService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "운영진 대시보드")
@RestController
@RequestMapping("/api/admin/dashboard")
@Validated
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardSummaryService dashboardSummaryService;
  private final RecentQuestionService recentQuestionService;
  private final SubmissionStatusService submissionStatusService;
  private final UpcomingEventService upcomingEventService;
  private final OngoingLectureService ongoingLectureService;

  @Operation(summary = "상단 카운터 4종", description = "명세서 5.1")
  @GetMapping("/summary")
  public ApiResponse<DashboardSummaryResult> getSummary() {
    return ApiResponse.success(dashboardSummaryService.getSummary());
  }

  @Operation(summary = "미확인 Q&A", description = "명세서 5.2")
  @GetMapping("/recent-questions")
  public ApiResponse<List<RecentQuestionResult>> getRecentQuestions(
      @RequestParam(defaultValue = "5") @Positive int size
  ) {
    return ApiResponse.success(recentQuestionService.getRecentQuestions(size));
  }

  @Operation(summary = "주차별 과제 제출 현황", description = "명세서 5.3")
  @GetMapping("/submission-status")
  public ApiResponse<SubmissionStatusResult> getSubmissionStatus(
      @RequestParam(required = false) Long trackId,
      @RequestParam(defaultValue = "5") @Positive int size
  ) {
    return ApiResponse.success(submissionStatusService.getSubmissionStatus(trackId, size));
  }

  @Operation(summary = "행사 일정 D-day", description = "명세서 5.4")
  @GetMapping("/upcoming-events")
  public ApiResponse<List<UpcomingEventResult>> getUpcomingEvents() {
    return ApiResponse.success(upcomingEventService.getUpcomingEvents());
  }

  @Operation(summary = "진행 중 강의", description = "명세서 5.5")
  @GetMapping("/ongoing-lectures")
  public ApiResponse<List<OngoingLectureResult>> getOngoingLectures() {
    return ApiResponse.success(ongoingLectureService.getOngoingLectures());
  }
}
