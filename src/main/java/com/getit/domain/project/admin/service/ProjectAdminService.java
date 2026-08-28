package com.getit.domain.project.admin.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.admin.dto.ProjectResult;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.exception.ProjectErrorCode;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectAdminService {

  private final ProjectRepository projectRepository;
  private final FileQueryService fileQueryService;

  /** 12.1. */
  public PageResponse<ProjectResult.Item> getProjects(String semester, Pageable pageable) {
    Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Project> page = projectRepository.searchBySemester(semester, pageOnly);
    Map<Long, String> thumbnails = resolveThumbnails(page.getContent());
    return PageResponse.from(page, project -> ProjectResult.Item.of(project, thumbnails.get(project.getId())));
  }

  /** 12.2. order 생략 시 맨 뒤로 붙인다. */
  @Transactional
  public ProjectResult.Item createProject(ProjectRequest.Write request) {
    int order = request.order() == null ? projectRepository.findMaxOrder() + 1 : request.order();
    Project saved = projectRepository.save(Project.create(request.toCommand(), order));
    return toItem(saved);
  }

  /** 12.3. */
  @Transactional
  public ProjectResult.Item updateProject(Long projectId, ProjectRequest.Write request) {
    Project project = findProject(projectId);
    project.update(request.toCommand());
    if (request.order() != null) {
      project.updateOrder(request.order());
    }
    return toItem(project);
  }

  private ProjectResult.Item toItem(Project project) {
    Map<Long, String> thumbnails = resolveThumbnails(List.of(project));
    return ProjectResult.Item.of(project, thumbnails.get(project.getId()));
  }

  /** 12.4. */
  @Transactional
  public void deleteProject(Long projectId) {
    projectRepository.delete(findProject(projectId));
  }

  private Project findProject(Long projectId) {
    return projectRepository.findById(projectId)
        .orElseThrow(() -> new BusinessException(ProjectErrorCode.PROJECT_NOT_FOUND));
  }

  private Map<Long, String> resolveThumbnails(List<Project> projects) {
    List<Long> fileIds = projects.stream()
        .map(Project::getFileId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (fileIds.isEmpty()) {
      return Map.of();
    }
    Map<Long, String> urlByFileId = fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));

    Map<Long, String> byProjectId = new HashMap<>();
    projects.forEach(project -> {
      if (project.getFileId() != null) {
        byProjectId.put(project.getId(), urlByFileId.get(project.getFileId()));
      }
    });
    return byProjectId;
  }
}
