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

  /**
   * generationId 를 생략하면 활성 기수를 조회한다. 정렬은 submittedAt 우선이지만 id 를 보조
   * 정렬 기준으로 함께 둔다 (PR #48 리뷰 지적 — submittedAt 만으로는 같은 시각에 제출된 지원자
   * 사이 순서가 페이지마다 바뀔 수 있고, status=DRAFT 로만 조회하면 submittedAt 이 전부 null 이라
   * 정렬 기준 자체가 없어진다).
   */
  @Operation(summary = "지원자 목록 조회", description = "명세서 7.1")
  @GetMapping
  public ApiResponse<PageResponse<ApplicantSummary>> getApplicants(
      @RequestParam(required = false) Long generationId,
      @RequestParam(required = false) ApplicationStatus status,
      @PageableDefault(size = 20, sort = {"submittedAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return ApiResponse.success(applicationAdminService.listApplicants(generationId, status, pageable));
  }

  @Operation(summary = "지원자 상세 조회", description = "명세서 7.2")
  @GetMapping("/{id}")
  public ApiResponse<ApplicantDetailResult> getApplicantDetail(@PathVariable Long id) {
    return ApiResponse.success(applicationAdminService.getApplicantDetail(id));
  }
}
