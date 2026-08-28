package com.getit.domain.project.service;

import com.getit.domain.project.repository.ProjectRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectQueryServiceImpl implements ProjectQueryService {

  private final ProjectRepository projectRepository;

  @Override
  public Page<ProjectView> findShowcase(String semester, Pageable pageable) {
    Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    return projectRepository.searchBySemester(semester, pageOnly).map(ProjectView::from);
  }

  @Override
  public List<String> findDistinctSemesters() {
    return projectRepository.findDistinctSemesters();
  }

  @Override
  public List<ProjectView> findFeatured() {
    return projectRepository.findByIsFeaturedTrueOrderByOrderAscIdAsc().stream()
        .map(ProjectView::from)
        .toList();
  }
}
