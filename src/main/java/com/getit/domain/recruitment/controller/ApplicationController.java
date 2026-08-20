package com.getit.domain.recruitment.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.MyApplicationResult;
import com.getit.domain.recruitment.service.ApplicationService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}
