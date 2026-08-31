package com.getit.domain.user.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.user.dto.PromoteRequest;
import com.getit.domain.user.dto.UserExportFilter;
import com.getit.domain.user.dto.UserPromotionResult;
import com.getit.domain.user.dto.UserSummary;
import com.getit.domain.user.dto.UserUpdateRequest;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.service.UserAdminService;
import com.getit.domain.user.service.UserPromotionService;
import com.getit.global.dto.ApiResponse;
import com.getit.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 관리")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

  private final UserAdminService userAdminService;
  private final UserPromotionService userPromotionService;

  @Operation(summary = "사용자 목록", description = "명세서 9.1")
  @GetMapping
  public ApiResponse<PageResponse<UserSummary>> getUsers(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) String groupId,
      @RequestParam(required = false) Integer generationNo,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return ApiResponse.success(userAdminService.listUsers(keyword, role, groupId, generationNo, pageable));
  }

  @Operation(summary = "권한 · 그룹 변경", description = "명세서 9.2")
  @PutMapping("/{id}")
  public ApiResponse<UserSummary> updateUser(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails principal,
      @RequestBody UserUpdateRequest request
  ) {
    return ApiResponse.success(userAdminService.updateUser(
        id, principal.getUserId(), request.role(), request.groupId(), request.generationNo(),
        request.unassignGroupOrDefault()));
  }

  @Operation(summary = "사용자 삭제", description = "명세서 9.3")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
    userAdminService.deleteUser(id, principal.getUserId());
  }

  @Operation(summary = "합격자 일괄 승격", description = "명세서 9.4")
  @PostMapping("/promote")
  public ApiResponse<UserPromotionResult> promote(@Valid @RequestBody PromoteRequest request) {
    return ApiResponse.success(userPromotionService.promote(request.generationId(), request.applicationIds()));
  }

  /** 바이너리(XLSX) 응답이라 {@code ApiResponse} envelope 을 쓰지 않는다. (7.6과 같은 이유) */
  @Operation(summary = "사용자 목록 엑셀 다운로드", description = "명세서 9.5")
  @GetMapping("/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) String groupId,
      @RequestParam(required = false) Integer generationNo
  ) {
    byte[] excel = userAdminService.exportUsersExcel(new UserExportFilter(keyword, role, groupId, generationNo));
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.xlsx")
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .body(excel);
  }
}
