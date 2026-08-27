package com.getit.domain.setting.generation.controller;

import com.getit.domain.setting.generation.dto.GenerationResult;
import com.getit.domain.setting.generation.dto.GenerationUpdateRequest;
import com.getit.domain.setting.generation.service.GenerationAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/generation")
@RequiredArgsConstructor
public class GenerationController {

  private final GenerationAdminService generationAdminService;

  @Operation(summary = "진행 기수 · 연도 조회", description = "명세서 10.1")
  @GetMapping
  public ApiResponse<GenerationResult> getGeneration() {
    return ApiResponse.success(generationAdminService.getActiveGeneration());
  }

  @Operation(summary = "진행 기수 · 연도 저장", description = "명세서 10.2")
  @PutMapping
  public ApiResponse<GenerationResult> updateGeneration(@Valid @RequestBody GenerationUpdateRequest request) {
    return ApiResponse.success(generationAdminService.updateGeneration(request.generationNo(), request.year()));
  }
}
