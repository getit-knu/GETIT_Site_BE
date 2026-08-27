package com.getit.domain.lecture.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.lecture.dto.SubmissionRequest;
import com.getit.domain.lecture.dto.SubmissionResult;
import com.getit.domain.lecture.service.SubmissionService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Submission", description = "과제 제출")
@RestController
@RequiredArgsConstructor
public class SubmissionController {

  private final SubmissionService submissionService;

  @Operation(summary = "과제 제출", description = "명세서 4.4")
  @PostMapping("/api/member/assignments/{assignmentId}/submissions")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SubmissionResult.Detail> submit(
      @PathVariable Long assignmentId,
      @Valid @RequestBody SubmissionRequest.Submit request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(submissionService.submit(assignmentId, request, principal.getUserId()));
  }

  @Operation(summary = "과제 재제출", description = "제출 덮어쓰기")
  @PutMapping("/api/member/submissions/{id}")
  public ApiResponse<SubmissionResult.Detail> resubmit(
      @PathVariable Long id,
      @Valid @RequestBody SubmissionRequest.Submit request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(submissionService.resubmit(id, request, principal.getUserId()));
  }
}
