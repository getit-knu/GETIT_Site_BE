package com.getit.domain.project.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ProjectRepositoryTest {

  @Autowired
  private ProjectRepository projectRepository;

  private Project project(String title, String semester, boolean isFeatured, int order) {
    ProjectCommand command = new ProjectCommand(
        title, "팀", semester, null, List.of(), null, null, isFeatured, null);
    return projectRepository.save(Project.create(command, order));
  }

  @Test
  @DisplayName("searchBySemester: 학기 필터 + order 오름차순")
  void searchBySemesterFiltersAndOrders() {
    project("B", "2025 Fall", false, 2);
    project("A", "2025 Fall", false, 1);
    project("C", "2024 Spring", false, 1);

    assertThat(projectRepository.searchBySemester("2025 Fall", PageRequest.of(0, 10)))
        .extracting(Project::getTitle)
        .containsExactly("A", "B");
  }

  @Test
  @DisplayName("searchBySemester: 학기가 null 이면 전체")
  void searchAllWhenSemesterNull() {
    project("A", "2025 Fall", false, 1);
    project("B", "2024 Spring", false, 2);

    assertThat(projectRepository.searchBySemester(null, PageRequest.of(0, 10))).hasSize(2);
  }

  @Test
  @DisplayName("findDistinctSemesters: 중복 없이 내림차순")
  void findDistinctSemesters() {
    project("A", "2025 Fall", false, 1);
    project("B", "2025 Fall", false, 2);
    project("C", "2024 Spring", false, 3);

    assertThat(projectRepository.findDistinctSemesters()).containsExactly("2025 Fall", "2024 Spring");
  }

  @Test
  @DisplayName("featured 만 order 순으로 조회한다")
  void findFeatured() {
    project("A", "2025 Fall", true, 2);
    project("B", "2025 Fall", true, 1);
    project("C", "2025 Fall", false, 1);

    assertThat(projectRepository.findByIsFeaturedTrueOrderByOrderAscIdAsc())
        .extracting(Project::getTitle)
        .containsExactly("B", "A");
  }

  @Test
  @DisplayName("findMaxOrder: 비어 있으면 0")
  void findMaxOrder() {
    assertThat(projectRepository.findMaxOrder()).isZero();
    project("A", "2025 Fall", false, 7);
    assertThat(projectRepository.findMaxOrder()).isEqualTo(7);
  }
}
