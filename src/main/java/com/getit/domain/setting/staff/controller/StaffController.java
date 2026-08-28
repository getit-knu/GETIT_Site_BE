package com.getit.domain.setting.staff.controller;

import com.getit.domain.setting.staff.dto.StaffOrderRequest;
import com.getit.domain.setting.staff.dto.StaffRequest;
import com.getit.domain.setting.staff.dto.StaffResult;
import com.getit.domain.setting.staff.service.StaffAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/staffs")
@RequiredArgsConstructor
public class StaffController {

  private final StaffAdminService staffAdminService;

  @Operation(summary = "운영진 목록", description = "명세서 10.21")
  @GetMapping
  public ApiResponse<List<StaffResult>> getStaffs() {
    return ApiResponse.success(staffAdminService.getStaffs());
  }

  @Operation(summary = "운영진 추가", description = "명세서 10.21")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<StaffResult> createStaff(@Valid @RequestBody StaffRequest request) {
    return ApiResponse.success(staffAdminService.createStaff(request));
  }

  @Operation(summary = "운영진 수정", description = "명세서 10.21")
  @PutMapping("/{id}")
  public ApiResponse<StaffResult> updateStaff(
      @PathVariable Long id,
      @Valid @RequestBody StaffRequest request
  ) {
    return ApiResponse.success(staffAdminService.updateStaff(id, request));
  }

  @Operation(summary = "운영진 삭제", description = "명세서 10.21")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteStaff(@PathVariable Long id) {
    staffAdminService.deleteStaff(id);
  }

  @Operation(summary = "운영진 순서 변경", description = "명세서 10.22")
  @PutMapping("/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderStaffs(@Valid @RequestBody StaffOrderRequest request) {
    staffAdminService.reorderStaffs(request.section(), request.orderedIds());
  }
}
