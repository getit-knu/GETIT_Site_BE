package com.getit.domain.recruitment.controller;

import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.service.ApplicationAdminService;
import com.getit.global.dto.ApiResponse;
import com.getit.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment", description = "모집")
@RestController
@RequestMapping("/api/admin/recruitment/applications")
@RequiredArgsConstructor
public class ApplicationAdminController {

  private final ApplicationAdminService applicationAdminService;

  @Operation(summary = "지원자 목록 조회", description = "명세서 7.1")
  @GetMapping
  public ApiResponse<PageResponse<ApplicantSummary>> getApplicants(
      @RequestParam(required = false) ApplicationStatus status,
      @PageableDefault(size = 20, sort = "submittedAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return ApiResponse.success(applicationAdminService.listApplicants(status, pageable));
  }

  @Operation(summary = "지원자 상세 조회", description = "명세서 7.2")
  @GetMapping("/{id}")
  public ApiResponse<ApplicantDetailResult> getApplicantDetail(@PathVariable Long id) {
    return ApiResponse.success(applicationAdminService.getApplicantDetail(id));
  }
}
