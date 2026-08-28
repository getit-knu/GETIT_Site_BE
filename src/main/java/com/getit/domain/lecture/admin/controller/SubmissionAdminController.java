package com.getit.domain.lecture.admin.controller;

import com.getit.domain.lecture.admin.dto.SubmissionDetailResult;
import com.getit.domain.lecture.admin.dto.SubmissionFilter;
import com.getit.domain.lecture.admin.dto.SubmissionOverviewResult;
import com.getit.domain.lecture.admin.service.SubmissionAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin.Submission", description = "과제 제출 현황")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SubmissionAdminController {

  private final SubmissionAdminService submissionAdminService;

  @Operation(summary = "과제 제출 현황", description = "명세서 8.6")
  @GetMapping("/lectures/{id}/submissions")
  public ApiResponse<SubmissionOverviewResult.Overview> getOverview(
      @PathVariable Long id,
      @RequestParam(required = false) Boolean submitted,
      @RequestParam(required = false) Boolean feedbackDone,
      @RequestParam(required = false) Long groupId,
      @PageableDefault(size = 50) Pageable pageable
  ) {
    return ApiResponse.success(submissionAdminService.getOverview(
        id, new SubmissionFilter(submitted, feedbackDone, groupId), pageable));
  }

  @Operation(summary = "제출물 상세", description = "명세서 8.7")
  @GetMapping("/submissions/{id}")
  public ApiResponse<SubmissionDetailResult.Detail> getDetail(@PathVariable Long id) {
    return ApiResponse.success(submissionAdminService.getDetail(id));
  }

  @Operation(summary = "피드백 순차 탐색", description = "명세서 8.10")
  @GetMapping("/lectures/{id}/submissions/navigate")
  public ApiResponse<SubmissionDetailResult.Navigation> navigate(
      @PathVariable Long id,
      @RequestParam Long currentSubmissionId,
      @RequestParam(required = false) Boolean submitted,
      @RequestParam(required = false) Boolean feedbackDone,
      @RequestParam(required = false) Long groupId
  ) {
    return ApiResponse.success(submissionAdminService.navigate(
        id, currentSubmissionId, new SubmissionFilter(submitted, feedbackDone, groupId)));
  }
}
