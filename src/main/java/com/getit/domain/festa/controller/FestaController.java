package com.getit.domain.festa.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.festa.dto.FestaPortfolioRequest;
import com.getit.domain.festa.dto.FestaPortfolioResult;
import com.getit.domain.festa.service.FestaPortfolioService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인한 사용자(GUEST 포함)가 쓴다. SecurityConfig 의 anyRequest().authenticated() 로 충분하다. */
@Tag(name = "Festa", description = "모의투자 FESTA")
@RestController
@RequestMapping("/api/festa/portfolio")
@RequiredArgsConstructor
public class FestaController {

  private final FestaPortfolioService festaPortfolioService;

  @Operation(summary = "내 가상 투자 현황", description = "이슈 #222")
  @GetMapping
  public ApiResponse<FestaPortfolioResult> getPortfolio(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(festaPortfolioService.getPortfolio(principal.getUserId()));
  }

  @Operation(summary = "가상 투자 저장(통째로 교체)", description = "이슈 #222")
  @PutMapping
  public ApiResponse<FestaPortfolioResult> savePortfolio(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody FestaPortfolioRequest request
  ) {
    return ApiResponse.success(festaPortfolioService.savePortfolio(principal.getUserId(), request));
  }
}
