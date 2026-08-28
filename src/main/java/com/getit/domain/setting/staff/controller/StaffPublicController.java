package com.getit.domain.setting.staff.controller;

import com.getit.domain.setting.staff.dto.StaffDirectoryResult;
import com.getit.domain.setting.staff.service.StaffPublicService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/staffs")
@RequiredArgsConstructor
public class StaffPublicController {

  private final StaffPublicService staffPublicService;

  @Operation(summary = "운영진 소개", description = "명세서 2.3")
  @GetMapping
  public ApiResponse<StaffDirectoryResult> getStaffDirectory() {
    return ApiResponse.success(staffPublicService.getStaffDirectory());
  }
}
