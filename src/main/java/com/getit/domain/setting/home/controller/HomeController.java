package com.getit.domain.setting.home.controller;

import com.getit.domain.setting.home.dto.HomeResult;
import com.getit.domain.setting.home.service.HomeService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/home")
@RequiredArgsConstructor
public class HomeController {

  private final HomeService homeService;

  @Operation(summary = "홈 통합 조회", description = "명세서 2.1")
  @GetMapping
  public ApiResponse<HomeResult> getHome() {
    return ApiResponse.success(homeService.getHome());
  }
}
