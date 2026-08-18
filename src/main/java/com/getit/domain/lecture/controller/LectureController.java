package com.getit.domain.lecture.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.lecture.dto.LectureRequest;
import com.getit.domain.lecture.dto.LectureResult;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.service.LectureService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin.Lecture", description = "강의")
@RestController
@RequestMapping("/api/admin/lectures")
@RequiredArgsConstructor
public class LectureController {

  private final LectureService lectureService;

  @Operation(summary = "강의 목록", description = "명세서 8.1")
  @GetMapping
  public ApiResponse<LectureResult.ListResult> getLectures(
      @RequestParam(required = false) Long generationId,
      @RequestParam(required = false) Long trackId,
      @RequestParam(required = false) Long subCategoryId
  ) {
    return ApiResponse.success(lectureService.getLectures(generationId, trackId, subCategoryId));
  }

  @Operation(summary = "강의 추가", description = "명세서 8.2")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<LectureResult.CreateResult> createLecture(
      @Valid @RequestBody LectureRequest.Create request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    Lecture lecture = lectureService.createLecture(request, principal.getUserId());
    return ApiResponse.success(LectureResult.CreateResult.from(lecture));
  }

  @Operation(summary = "강의 단건 조회", description = "명세서 8.3")
  @GetMapping("/{id}")
  public ApiResponse<LectureResult.DetailResult> getLecture(@PathVariable Long id) {
    return ApiResponse.success(lectureService.getLecture(id));
  }
}
