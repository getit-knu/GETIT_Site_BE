package com.getit.domain.lecture.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.lecture.dto.MeSummaryResult;
import com.getit.domain.lecture.service.MeSummaryService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Me", description = "부원 내 정보")
@RestController
@RequestMapping("/api/member/me")
@RequiredArgsConstructor
public class MeController {

  private final MeSummaryService meSummaryService;

  @Operation(summary = "내 정보·학습 통계", description = "명세서 4.5")
  @GetMapping("/summary")
  public ApiResponse<MeSummaryResult.Response> getSummary(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(meSummaryService.getSummary(principal.getUserId()));
  }
}
