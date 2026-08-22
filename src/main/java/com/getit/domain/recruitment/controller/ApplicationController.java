package com.getit.domain.recruitment.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.recruitment.dto.ApplicationDecisionResult;
import com.getit.domain.recruitment.dto.ApplicationDraftRequest;
import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.DraftSaveResult;
import com.getit.domain.recruitment.dto.MyApplicationResult;
import com.getit.domain.recruitment.dto.SubmitResult;
import com.getit.domain.recruitment.service.ApplicationService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Application", description = "지원서 (지원자)")
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService applicationService;

  @Operation(summary = "지원서 양식 조회", description = "명세서 3.1")
  @GetMapping("/form")
  public ApiResponse<ApplicationFormResult> getForm(@AuthenticationPrincipal CustomUserDetails principal) {
    return ApiResponse.success(applicationService.getForm(principal.getUserId()));
  }

  @Operation(summary = "내 지원서 조회", description = "명세서 3.2")
  @GetMapping("/me")
  public ApiResponse<MyApplicationResult> getMyApplication(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(applicationService.getMyApplication(principal.getUserId()));
  }

  @Operation(summary = "지원서 임시 저장", description = "명세서 3.3")
  @PutMapping("/me/draft")
  public ApiResponse<DraftSaveResult> saveDraft(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody ApplicationDraftRequest request
  ) {
    return ApiResponse.success(applicationService.saveDraft(principal.getUserId(), request));
  }

  /** 본문 없이 저장된 draft 를 그대로 제출하는 것도 허용한다. (명세서 3.4) */
  @Operation(summary = "지원서 제출", description = "명세서 3.4")
  @PostMapping("/me/submit")
  public ApiResponse<SubmitResult> submit(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody(required = false) ApplicationDraftRequest request
  ) {
    return ApiResponse.success(applicationService.submit(principal.getUserId(), request));
  }

  @Operation(summary = "지원서 결과 조회", description = "명세서 3.5")
  @GetMapping("/me/result")
  public ApiResponse<ApplicationDecisionResult> getResult(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(applicationService.getResult(principal.getUserId()));
  }
}
