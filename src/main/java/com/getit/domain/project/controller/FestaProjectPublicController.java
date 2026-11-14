package com.getit.domain.project.controller;

import com.getit.domain.project.dto.ProjectShowcaseResult;
import com.getit.domain.project.service.ProjectPublicService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/festa/projects")
@RequiredArgsConstructor
public class FestaProjectPublicController {

  private final ProjectPublicService projectPublicService;

  @Operation(summary = "FESTA 출전 프로젝트", description = "명세 없음 (이슈 #220)")
  @GetMapping
  public ApiResponse<List<ProjectShowcaseResult.Item>> getFestaProjects() {
    return ApiResponse.success(projectPublicService.getFestaProjects());
  }
}
