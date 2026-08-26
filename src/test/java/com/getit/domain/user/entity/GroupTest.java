package com.getit.domain.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GroupTest {

  @Test
  @DisplayName("조를 생성한다")
  void createsGroup() {
    Group group = Group.create(1L, "1조");

    assertThat(group.getGenerationId()).isEqualTo(1L);
    assertThat(group.getName()).isEqualTo("1조");
  }

  @Test
  @DisplayName("이름을 수정하면 소속 기수는 바뀌지 않는다")
  void renameKeepsGenerationId() {
    Group group = Group.create(1L, "1조");

    group.rename("A조");

    assertThat(group.getName()).isEqualTo("A조");
    assertThat(group.getGenerationId()).isEqualTo(1L);
  }
}
