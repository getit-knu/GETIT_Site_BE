package com.getit.domain.setting.photo.controller;

import java.util.List;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.getit.domain.setting.photo.dto.ActivityPhotoRequest;
import com.getit.domain.setting.photo.dto.ActivityPhotoResult;
import com.getit.domain.setting.photo.service.ActivityPhotoAdminService;
import com.getit.global.dto.ApiResponse;

@Tag(name = "Admin 활동 사진", description = "홈 화면 활동 사진 마퀴 관리")
@RestController
@RequestMapping("/api/admin/setting/activity-photos")
@RequiredArgsConstructor
public class ActivityPhotoController {

  private final ActivityPhotoAdminService activityPhotoAdminService;

  @Operation(summary = "활동 사진 목록", description = "숨긴 사진도 포함해 순서대로 반환한다.")
  @GetMapping
  public ApiResponse<List<ActivityPhotoResult>> getPhotos() {
    return ApiResponse.success(activityPhotoAdminService.getPhotos());
  }

  @Operation(summary = "활동 사진 등록",
      description = "미리 업로드한 fileId 를 연결한다. order 를 비우면 맨 뒤에 붙는다.")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ActivityPhotoResult> createPhoto(
      @Valid @RequestBody ActivityPhotoRequest request
  ) {
    return ApiResponse.success(activityPhotoAdminService.create(request));
  }

  @Operation(summary = "활동 사진 수정", description = "사진 교체 · 노출 여부 · 순서를 바꾼다.")
  @PutMapping("/{id}")
  public ApiResponse<ActivityPhotoResult> updatePhoto(
      @PathVariable Long id,
      @Valid @RequestBody ActivityPhotoRequest request
  ) {
    return ApiResponse.success(activityPhotoAdminService.update(id, request));
  }

  @Operation(summary = "활동 사진 삭제")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePhoto(@PathVariable Long id) {
    activityPhotoAdminService.delete(id);
  }
}
