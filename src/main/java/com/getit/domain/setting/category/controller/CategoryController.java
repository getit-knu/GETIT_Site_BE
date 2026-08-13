package com.getit.domain.setting.category.controller;

import com.getit.domain.setting.category.dto.CategoryRequest.SubCategoryCreate;
import com.getit.domain.setting.category.dto.CategoryRequest.SubCategoryUpdate;
import com.getit.domain.setting.category.dto.CategoryRequest.TrackCreate;
import com.getit.domain.setting.category.dto.CategoryRequest.TrackUpdate;
import com.getit.domain.setting.category.dto.CategoryResponse.SubCategoryResult;
import com.getit.domain.setting.category.dto.CategoryResponse.TrackResult;
import com.getit.domain.setting.category.dto.CategoryTreeResult.TrackNode;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.service.CategoryService;
import com.getit.domain.setting.category.service.CategoryUsageChecker;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@Tag(name = "Category", description = "강의 분류")
@RestController
@RequestMapping("/api/admin/setting")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;
  private final CategoryUsageChecker categoryUsageChecker;

  @Operation(summary = "강의 분류 트리 조회", description = "명세서 10.3")
  @GetMapping("/tracks")
  public ApiResponse<List<TrackNode>> getCategoryTree() {
    return ApiResponse.success(categoryService.getCategoryTree());
  }

  @Operation(summary = "대분류 추가", description = "명세서 10.4")
  @PostMapping("/tracks")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<TrackResult> createTrack(@Valid @RequestBody TrackCreate request) {
    Track track = categoryService.createTrack(request.name());
    return ApiResponse.success(TrackResult.of(track, 0));
  }

  @Operation(summary = "대분류 수정", description = "명세서 10.5")
  @PutMapping("/tracks/{id}")
  public ApiResponse<TrackResult> updateTrack(@PathVariable Long id, @Valid @RequestBody TrackUpdate request) {
    Track track = categoryService.updateTrack(id, request.name(), request.order());
    long lectureCount = categoryUsageChecker.countLecturesByTrackId(id);

    return ApiResponse.success(TrackResult.of(track, lectureCount));
  }

  @Operation(summary = "대분류 삭제", description = "명세서 10.6")
  @DeleteMapping("/tracks/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteTrack(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
    categoryService.deleteTrack(id, force);
  }

  @Operation(summary = "소분류 추가", description = "명세서 10.7")
  @PostMapping("/subcategories")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SubCategoryResult> createSubCategory(@Valid @RequestBody SubCategoryCreate request) {
    SubCategory subCategory = categoryService.createSubCategory(request.trackId(), request.name());
    return ApiResponse.success(SubCategoryResult.of(subCategory, 0));
  }

  @Operation(summary = "소분류 수정", description = "명세서 10.8")
  @PutMapping("/subcategories/{id}")
  public ApiResponse<SubCategoryResult> updateSubCategory(
      @PathVariable Long id, @Valid @RequestBody SubCategoryUpdate request
  ) {
    SubCategory subCategory = categoryService.updateSubCategory(id, request.name(), request.order());
    long lectureCount = categoryUsageChecker.countLecturesBySubCategoryId(id);

    return ApiResponse.success(SubCategoryResult.of(subCategory, lectureCount));
  }

  @Operation(summary = "소분류 삭제", description = "명세서 10.9")
  @DeleteMapping("/subcategories/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSubCategory(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean force) {
    categoryService.deleteSubCategory(id, force);
  }
}
