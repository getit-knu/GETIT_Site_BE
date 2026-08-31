package com.getit.domain.project.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
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

  /** 부원이 낸 승인 대기 프로젝트. */
  private Project pending(String title, String semester, int order) {
    ProjectCommand command = new ProjectCommand(
        title, "팀", semester, null, List.of(), null, null, false, null);
    return projectRepository.save(Project.submit(command, order, null));
  }

  @Test
  @DisplayName("searchBySemester: 학기 필터 + order 오름차순")
  void searchBySemesterFiltersAndOrders() {
    project("B", "2025-FALL", false, 2);
    project("A", "2025-FALL", false, 1);
    project("C", "2024-SPRING", false, 1);

    assertThat(projectRepository.searchBySemester("2025-FALL", null, PageRequest.of(0, 10)))
        .extracting(Project::getTitle)
        .containsExactly("A", "B");
  }

  @Test
  @DisplayName("searchBySemester: 학기가 null 이면 전체")
  void searchAllWhenSemesterNull() {
    project("A", "2025-FALL", false, 1);
    project("B", "2024-SPRING", false, 2);

    assertThat(projectRepository.searchBySemester(null, null, PageRequest.of(0, 10))).hasSize(2);
  }

  @Test
  @DisplayName("findDistinctSemesters: 중복 없이 내림차순")
  void findDistinctSemesters() {
    project("A", "2025-FALL", false, 1);
    project("B", "2025-FALL", false, 2);
    project("C", "2024-SPRING", false, 3);

    assertThat(projectRepository.findDistinctSemestersByStatus(ProjectStatus.APPROVED))
        .containsExactly("2025-FALL", "2024-SPRING");
  }

  @Test
  @DisplayName("featured 만 order 순으로 조회한다")
  void findFeatured() {
    project("A", "2025-FALL", true, 2);
    project("B", "2025-FALL", true, 1);
    project("C", "2025-FALL", false, 1);

    assertThat(projectRepository.findByIsFeaturedTrueAndStatusOrderByOrderAscIdAsc(
        ProjectStatus.APPROVED))
        .extracting(Project::getTitle)
        .containsExactly("B", "A");
  }

  @Test
  @DisplayName("searchBySemester: status 로 승인 대기만 골라낸다")
  void searchBySemesterFiltersByStatus() {
    project("승인됨", "2025-FALL", false, 1);
    pending("대기중", "2025-FALL", 2);

    assertThat(projectRepository.searchBySemester(null, ProjectStatus.PENDING, PageRequest.of(0, 10)))
        .extracting(Project::getTitle)
        .containsExactly("대기중");
    // 어드민 목록은 status 를 null 로 넘겨 전부 본다.
    assertThat(projectRepository.searchBySemester(null, null, PageRequest.of(0, 10))).hasSize(2);
  }

  @Test
  @DisplayName("공개 조회는 승인 대기 · 반려된 프로젝트를 내보내지 않는다")
  void publicQueriesHidePendingProjects() {
    pending("대기중", "2025-FALL", 1);

    assertThat(projectRepository.searchBySemester(
        null, ProjectStatus.APPROVED, PageRequest.of(0, 10))).isEmpty();
    assertThat(projectRepository.findDistinctSemestersByStatus(ProjectStatus.APPROVED)).isEmpty();
  }

  @Test
  @DisplayName("order 가 겹쳐도 id 로 갈려 정렬이 흔들리지 않는다")
  void breaksOrderTiesById() {
    // 동시 제출 둘이 같은 max + 1 을 읽으면 order 가 겹칠 수 있다 (PR #165 리뷰).
    Project first = project("먼저", "2025-FALL", false, 1);
    Project second = project("나중", "2025-FALL", false, 1);

    assertThat(projectRepository.searchBySemester(null, null, PageRequest.of(0, 10)))
        .extracting(Project::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("findMaxOrder: 비어 있으면 0")
  void findMaxOrder() {
    assertThat(projectRepository.findMaxOrder()).isZero();
    project("A", "2025-FALL", false, 7);
    assertThat(projectRepository.findMaxOrder()).isEqualTo(7);
  }
}
