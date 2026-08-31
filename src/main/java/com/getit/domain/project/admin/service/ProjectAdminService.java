package com.getit.domain.project.admin.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.admin.dto.ProjectResult;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
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

  /**
   * @param status 생략하면 전체. 어드민은 승인 대기 중인 것도 봐야 하므로 기본이 전체다
   *               (이슈 #148). 다만 "승인 대기만 모아 보기" 가 필요해 필터를 열었다 —
   *               화면에서 거르면 현재 페이지 안에서만 걸러져 페이지를 넘길 때마다 결과가
   *               달라진다 (이슈 #190)
   */
  public PageResponse<ProjectResult.Item> getProjects(
      String semester, ProjectStatus status, Pageable pageable
  ) {
    Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Project> page = projectRepository.searchBySemester(semester, status, pageOnly);
    Map<Long, String> thumbnails = resolveThumbnails(page.getContent());
    return PageResponse.from(page, project -> ProjectResult.Item.of(project, thumbnails.get(project.getId())));
  }

  @Transactional
  public ProjectResult.Item createProject(ProjectRequest.Write request) {
    int order = request.order() == null ? projectRepository.findMaxOrder() + 1 : request.order();
    Project saved = projectRepository.save(Project.create(request.toCommand(), order));
    return toItem(saved);
  }

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

  /**
   * 부원이 낸 프로젝트를 승인하거나 반려한다. (이슈 #148)
   *
   * <p>반려한 것을 다시 승인하는 것도 허용한다. 사람이 판단을 바꿀 수 있어야 한다.
   * 이미 같은 상태면 막는다 — 눌렀는데 아무 일도 일어나지 않는 것보다 낫다.
   */
  @Transactional
  public ProjectResult.Item changeStatus(Long projectId, ProjectStatus status) {
    Project project = findProject(projectId);
    if (project.getStatus() == status) {
      throw new BusinessException(ProjectErrorCode.PROJECT_STATUS_UNCHANGED);
    }
    project.changeStatus(status);
    return toItem(project);
  }

  /**
   * 사유를 남기며 반려한다. (이슈 #190)
   *
   * <p>사유 없이 반려하면 부원은 반려된 것만 알고 이유를 모른다. 그러면 같은 이유로 다시
   * 낸다 — 이 기능이 있는 이유가 그것이다.
   *
   * <p>다시 승인하면 사유는 비워진다({@link Project#changeStatus}). 공개된 프로젝트에 반려
   * 사유가 붙어 있는 상태를 만들지 않는다.
   */
  @Transactional
  public ProjectResult.Item reject(Long projectId, String reason) {
    Project project = findProject(projectId);
    if (project.getStatus() == ProjectStatus.REJECTED) {
      throw new BusinessException(ProjectErrorCode.PROJECT_STATUS_UNCHANGED);
    }
    project.reject(reason);
    return toItem(project);
  }

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
