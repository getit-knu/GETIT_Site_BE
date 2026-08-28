package com.getit.domain.recruitment.controller;

import com.getit.domain.recruitment.dto.RecruitmentStatusResult;
import com.getit.domain.recruitment.service.RecruitmentStatusService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/recruitment/status")
@RequiredArgsConstructor
public class RecruitmentStatusController {

  private final RecruitmentStatusService recruitmentStatusService;

  @Operation(summary = "모집 상태 · D-day", description = "명세서 2.8")
  @GetMapping
  public ApiResponse<RecruitmentStatusResult> getStatus() {
    return ApiResponse.success(recruitmentStatusService.getStatus());
  }
}
