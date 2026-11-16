package com.getit.domain.festa.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.festa.dto.FestaVoteRequest;
import com.getit.domain.festa.dto.FestaVoteResult;
import com.getit.domain.festa.service.FestaVoteService;
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
@RequestMapping("/api/festa/vote")
@RequiredArgsConstructor
public class FestaVoteController {

  private final FestaVoteService festaVoteService;

  @Operation(summary = "내 투표 현황", description = "이슈 #232")
  @GetMapping
  public ApiResponse<FestaVoteResult> getVote(@AuthenticationPrincipal CustomUserDetails principal) {
    return ApiResponse.success(festaVoteService.getVote(principal.getUserId()));
  }

  @Operation(summary = "투표하기, 바꾸기", description = "이슈 #232")
  @PutMapping
  public ApiResponse<FestaVoteResult> vote(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody FestaVoteRequest request
  ) {
    return ApiResponse.success(festaVoteService.vote(principal.getUserId(), request));
  }
}
