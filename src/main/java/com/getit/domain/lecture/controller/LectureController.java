package com.getit.domain.lecture.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.lecture.dto.LectureResult;
import com.getit.domain.lecture.service.LectureService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Lecture", description = "부원 강의 조회")
@RestController
@RequestMapping("/api/member/lectures")
@RequiredArgsConstructor
public class LectureController {

  private final LectureService lectureService;

  @Operation(summary = "부원 강의 목록", description = "명세서 4.1")
  @GetMapping
  public ApiResponse<LectureResult.ListResult> getLectures(
      @RequestParam(required = false) Long trackId,
      @RequestParam(required = false) Long subCategoryId,
      @PageableDefault(size = 12) Pageable pageable,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(
        lectureService.getLectures(principal.getUserId(), trackId, subCategoryId, pageable));
  }

  @Operation(summary = "부원 강의 상세", description = "명세서 4.2")
  @GetMapping("/{id}")
  public ApiResponse<LectureResult.DetailResult> getLecture(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(lectureService.getLecture(principal.getUserId(), id));
  }
}
