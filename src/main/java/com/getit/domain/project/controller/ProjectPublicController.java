package com.getit.domain.project.controller;

import com.getit.domain.project.dto.ProjectShowcaseResult;
import com.getit.domain.project.service.ProjectPublicService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/projects")
@RequiredArgsConstructor
public class ProjectPublicController {

  private final ProjectPublicService projectPublicService;

  @Operation(summary = "프로젝트 쇼케이스", description = "명세서 2.4")
  @GetMapping
  public ApiResponse<ProjectShowcaseResult> getShowcase(
      @RequestParam(required = false) String semester,
      @PageableDefault(size = 9) Pageable pageable
  ) {
    return ApiResponse.success(projectPublicService.getShowcase(semester, pageable));
  }
}
