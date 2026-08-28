package com.getit.domain.project.admin.controller;

import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.admin.dto.ProjectResult;
import com.getit.domain.project.admin.service.ProjectAdminService;
import com.getit.global.dto.ApiResponse;
import com.getit.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Admin.Project", description = "프로젝트 관리")
@RestController
@RequestMapping("/api/admin/projects")
@RequiredArgsConstructor
public class ProjectAdminController {

  private final ProjectAdminService projectAdminService;

  @Operation(summary = "프로젝트 목록", description = "명세서 12.1")
  @GetMapping
  public ApiResponse<PageResponse<ProjectResult.Item>> getProjects(
      @RequestParam(required = false) String semester,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return ApiResponse.success(projectAdminService.getProjects(semester, pageable));
  }

  @Operation(summary = "프로젝트 등록", description = "명세서 12.2")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ProjectResult.Item> createProject(@Valid @RequestBody ProjectRequest.Write request) {
    return ApiResponse.success(projectAdminService.createProject(request));
  }

  @Operation(summary = "프로젝트 수정", description = "명세서 12.3")
  @PutMapping("/{id}")
  public ApiResponse<ProjectResult.Item> updateProject(
      @PathVariable Long id,
      @Valid @RequestBody ProjectRequest.Write request
  ) {
    return ApiResponse.success(projectAdminService.updateProject(id, request));
  }

  @Operation(summary = "프로젝트 삭제", description = "명세서 12.4")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteProject(@PathVariable Long id) {
    projectAdminService.deleteProject(id);
  }
}
