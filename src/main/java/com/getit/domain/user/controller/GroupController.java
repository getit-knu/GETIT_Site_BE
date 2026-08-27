package com.getit.domain.user.controller;

import com.getit.domain.user.dto.GroupBoardResult;
import com.getit.domain.user.dto.GroupCreateRequest;
import com.getit.domain.user.dto.GroupMemberAddRequest;
import com.getit.domain.user.dto.GroupMemberAddResult;
import com.getit.domain.user.dto.GroupRenameRequest;
import com.getit.domain.user.dto.GroupResult;
import com.getit.domain.user.service.GroupService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

@Tag(name = "User", description = "사용자 · 조 관리")
@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
public class GroupController {

  private final GroupService groupService;

  @Operation(summary = "조 목록 + 조원", description = "명세서 9.6")
  @GetMapping
  public ApiResponse<GroupBoardResult> getGroups(
      @RequestParam(required = false) Long generationId
  ) {
    return ApiResponse.success(groupService.getGroups(generationId));
  }

  @Operation(summary = "조 생성", description = "명세서 9.7")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<GroupResult> createGroup(@Valid @RequestBody GroupCreateRequest request) {
    return ApiResponse.success(groupService.createGroup(request.generationId(), request.name()));
  }

  @Operation(summary = "조 이름 수정", description = "명세서 9.8")
  @PutMapping("/{id}")
  public ApiResponse<GroupResult> renameGroup(
      @PathVariable Long id,
      @Valid @RequestBody GroupRenameRequest request
  ) {
    return ApiResponse.success(groupService.renameGroup(id, request.name()));
  }

  @Operation(summary = "조 삭제", description = "명세서 9.9")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteGroup(@PathVariable Long id) {
    groupService.deleteGroup(id);
  }

  @Operation(summary = "조원 추가", description = "명세서 9.10")
  @PostMapping("/{groupId}/members")
  public ApiResponse<GroupMemberAddResult> addMembers(
      @PathVariable Long groupId,
      @Valid @RequestBody GroupMemberAddRequest request
  ) {
    return ApiResponse.success(groupService.addMembers(groupId, request.userIds()));
  }

  @Operation(summary = "조원 빼기", description = "명세서 9.11")
  @DeleteMapping("/{groupId}/members/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeMember(@PathVariable Long groupId, @PathVariable Long userId) {
    groupService.removeMember(groupId, userId);
  }
}
