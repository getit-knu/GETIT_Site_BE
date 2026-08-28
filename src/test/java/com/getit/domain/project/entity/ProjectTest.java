package com.getit.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.project.dto.ProjectCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProjectTest {

  private ProjectCommand command(String title, boolean isFeatured) {
    return new ProjectCommand(title, "팀", "2025-FALL", "설명",
        List.of("React", "Spring"), "https://code", "https://demo", isFeatured, null);
  }

  @Test
  @DisplayName("생성한다")
  void creates() {
    Project project = Project.create(command("쇼케이스", true), 3);

    assertThat(project.getTitle()).isEqualTo("쇼케이스");
    assertThat(project.getOrder()).isEqualTo(3);
    assertThat(project.isFeatured()).isTrue();
    assertThat(project.getTechStacks()).containsExactly("React", "Spring");
  }

  @Test
  @DisplayName("수정: order 는 그대로다")
  void updates() {
    Project project = Project.create(command("원래", true), 3);

    project.update(command("변경", false));

    assertThat(project.getTitle()).isEqualTo("변경");
    assertThat(project.isFeatured()).isFalse();
    assertThat(project.getOrder()).isEqualTo(3);
  }

  @Test
  @DisplayName("순서를 변경한다")
  void updatesOrder() {
    Project project = Project.create(command("쇼케이스", true), 3);

    project.updateOrder(5);

    assertThat(project.getOrder()).isEqualTo(5);
  }
}
