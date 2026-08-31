package com.getit.domain.user.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.service.MemberGroupService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Group", description = "부원 내 조")
@RestController
@RequestMapping("/api/member/group")
@RequiredArgsConstructor
public class MemberGroupController {

  private final MemberGroupService memberGroupService;

  /**
   * 내 조와 조원. (이슈 #148)
   *
   * <p>아직 조에 배정되지 않았으면 {@code data} 가 null 이다. 배정 전은 오류가 아니라
   * 정상 상태이므로 404 로 알리지 않는다.
   */
  @Operation(summary = "내 조 조회", description = "이슈 #148")
  @GetMapping
  public ApiResponse<GroupWithMembersResult> getMyGroup(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(memberGroupService.findMyGroup(principal.getUserId()).orElse(null));
  }
}
