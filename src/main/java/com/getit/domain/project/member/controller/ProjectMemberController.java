package com.getit.domain.project.member.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.project.member.dto.MemberProjectResult;
import com.getit.domain.project.member.dto.ProjectSubmitRequest;
import com.getit.domain.project.member.service.ProjectMemberService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Project", description = "부원 프로젝트")
@RestController
@RequestMapping("/api/member/projects")
@RequiredArgsConstructor
public class ProjectMemberController {

  private final ProjectMemberService projectMemberService;

  /**
   * 내 조 명의로 프로젝트 등록. (이슈 #148)
   *
   * <p>바로 공개되지 않는다. 승인 대기 상태로 들어가고 어드민이 승인해야 쇼케이스에 나온다.
   */
  @Operation(summary = "프로젝트 등록(승인 대기)", description = "이슈 #148")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<MemberProjectResult> submitProject(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody ProjectSubmitRequest request
  ) {
    return ApiResponse.success(projectMemberService.submitProject(principal.getUserId(), request));
  }
}
