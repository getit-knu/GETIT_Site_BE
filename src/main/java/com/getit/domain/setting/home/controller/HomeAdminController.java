package com.getit.domain.setting.home.controller;

import com.getit.domain.setting.home.dto.HomeSaveRequest;
import com.getit.domain.setting.home.dto.HomeSaveResult;
import com.getit.domain.setting.home.service.HomeSaveService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/home")
@RequiredArgsConstructor
public class HomeAdminController {

  private final HomeSaveService homeSaveService;

  @Operation(summary = "홈 화면 일괄 저장", description = "명세서 10.20")
  @PostMapping("/save")
  public ApiResponse<HomeSaveResult> save(
      @Valid @RequestBody HomeSaveRequest request,
      @RequestParam(defaultValue = "false") boolean force
  ) {
    return ApiResponse.success(homeSaveService.save(request, force));
  }
}
