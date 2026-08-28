package com.getit.domain.project.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.dto.ProjectShowcaseResult;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectPublicService {

  private final ProjectQueryService projectQueryService;
  private final FileQueryService fileQueryService;

  public ProjectShowcaseResult getShowcase(String semester, Pageable pageable) {
    String normalized = semester == null || semester.isBlank() ? null : semester;
    Page<ProjectView> page = projectQueryService.findShowcase(normalized, pageable);

    Map<Long, String> urlByFileId = resolveThumbnails(page.getContent());
    List<ProjectShowcaseResult.Item> content = page.getContent().stream()
        .map(view -> toItem(view, urlByFileId))
        .toList();

    return new ProjectShowcaseResult(
        projectQueryService.findDistinctSemesters(),
        content,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }

  private Map<Long, String> resolveThumbnails(List<ProjectView> views) {
    List<Long> fileIds = views.stream()
        .map(ProjectView::fileId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (fileIds.isEmpty()) {
      return Map.of();
    }
    return fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));
  }

  private ProjectShowcaseResult.Item toItem(ProjectView view, Map<Long, String> urlByFileId) {
    String thumbnailUrl = view.fileId() == null ? null : urlByFileId.get(view.fileId());
    return new ProjectShowcaseResult.Item(
        view.id(), view.title(), view.teamName(), view.semester(), view.semester(),
        view.description(), view.techStacks(), view.codeUrl(), view.demoUrl(), thumbnailUrl);
  }
}
