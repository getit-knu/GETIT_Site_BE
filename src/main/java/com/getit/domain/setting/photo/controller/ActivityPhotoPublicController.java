package com.getit.domain.setting.photo.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.getit.domain.setting.photo.dto.ActivityPhotoPublicResult;
import com.getit.domain.setting.photo.service.ActivityPhotoPublicService;
import com.getit.global.dto.ApiResponse;

@Tag(name = "Public 활동 사진", description = "홈 화면 활동 사진 마퀴")
@RestController
@RequestMapping("/api/public/activity-photos")
@RequiredArgsConstructor
public class ActivityPhotoPublicController {

  private final ActivityPhotoPublicService activityPhotoPublicService;

  @Operation(summary = "활동 사진 조회", description = "노출로 설정된 사진만 순서대로 반환한다.")
  @GetMapping
  public ApiResponse<List<ActivityPhotoPublicResult>> getPhotos() {
    return ApiResponse.success(activityPhotoPublicService.getPhotos());
  }
}
