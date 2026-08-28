package com.getit.domain.setting.curriculum.controller;

import com.getit.domain.setting.curriculum.dto.CurriculumRequest;
import com.getit.domain.setting.curriculum.dto.CurriculumResult;
import com.getit.domain.setting.curriculum.service.CurriculumAdminService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/curriculums")
@RequiredArgsConstructor
public class CurriculumController {

  private final CurriculumAdminService curriculumAdminService;

  @Operation(summary = "커리큘럼 목록", description = "명세서 10.10")
  @GetMapping
  public ApiResponse<List<CurriculumResult>> getCurriculums() {
    return ApiResponse.success(curriculumAdminService.getCurriculums());
  }

  @Operation(summary = "커리큘럼 추가", description = "명세서 10.11")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<CurriculumResult> createCurriculum(@Valid @RequestBody CurriculumRequest request) {
    return ApiResponse.success(curriculumAdminService.createCurriculum(request));
  }

  @Operation(summary = "커리큘럼 수정", description = "명세서 10.12")
  @PutMapping("/{id}")
  public ApiResponse<CurriculumResult> updateCurriculum(
      @PathVariable Long id,
      @Valid @RequestBody CurriculumRequest request
  ) {
    return ApiResponse.success(curriculumAdminService.updateCurriculum(id, request));
  }

  @Operation(summary = "커리큘럼 삭제", description = "명세서 10.13")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCurriculum(@PathVariable Long id) {
    curriculumAdminService.deleteCurriculum(id);
  }
}
