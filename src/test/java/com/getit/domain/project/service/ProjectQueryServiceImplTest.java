package com.getit.domain.project.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.repository.ProjectRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProjectQueryServiceImplTest {

  @Autowired
  private ProjectQueryService projectQueryService;

  @Autowired
  private ProjectRepository projectRepository;

  private void seed(String title, String semester, boolean isFeatured, int order) {
    ProjectCommand command = new ProjectCommand(
        title, "팀", semester, null, List.of("React"), null, null, isFeatured, null);
    projectRepository.save(Project.create(command, order));
  }

  @Test
  @DisplayName("findShowcase: 학기 필터 + order 순 + 페이지")
  void findShowcase() {
    seed("B", "2025-FALL", false, 2);
    seed("A", "2025-FALL", false, 1);
    seed("C", "2024-SPRING", false, 1);

    var page = projectQueryService.findShowcase("2025-FALL", PageRequest.of(0, 9));

    assertThat(page.getContent()).extracting(ProjectView::title).containsExactly("A", "B");
  }

  @Test
  @DisplayName("findDistinctSemesters: 실존 학기만")
  void findDistinctSemesters() {
    seed("A", "2025-FALL", false, 1);
    seed("B", "2025-FALL", false, 2);
    seed("C", "2024-SPRING", false, 3);

    assertThat(projectQueryService.findDistinctSemesters()).containsExactly("2025-FALL", "2024-SPRING");
  }

  @Test
  @DisplayName("findFeatured: isFeatured 만 order 순으로")
  void findFeatured() {
    seed("A", "2025-FALL", true, 2);
    seed("B", "2025-FALL", true, 1);
    seed("C", "2025-FALL", false, 1);

    assertThat(projectQueryService.findFeatured()).extracting(ProjectView::title).containsExactly("B", "A");
  }
}
