package com.getit.domain.lecture.admin.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.lecture.admin.dto.FeedbackRequest;
import com.getit.domain.lecture.admin.dto.FeedbackResult;
import com.getit.domain.lecture.admin.service.FeedbackService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin.Feedback", description = "과제 피드백")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class FeedbackController {

  private final FeedbackService feedbackService;

  @Operation(summary = "피드백 작성", description = "명세서 8.8")
  @PostMapping("/submissions/{id}/feedback")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<FeedbackResult.CreateResult> create(
      @PathVariable Long id,
      @Valid @RequestBody FeedbackRequest.Write request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(feedbackService.create(id, request, principal.getUserId()));
  }

  @Operation(summary = "피드백 수정", description = "명세서 8.9")
  @PutMapping("/feedbacks/{feedbackId}")
  public ApiResponse<FeedbackResult.UpdateResult> update(
      @PathVariable Long feedbackId,
      @Valid @RequestBody FeedbackRequest.Write request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(feedbackService.update(feedbackId, request, principal.getUserId()));
  }
}
