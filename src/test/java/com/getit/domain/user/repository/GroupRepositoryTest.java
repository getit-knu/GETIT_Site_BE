package com.getit.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.entity.Group;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class GroupRepositoryTest {

  @Autowired
  private GroupRepository groupRepository;

  @Test
  @DisplayName("기수별로 생성 순서대로 조회한다")
  void findsByGenerationIdOrderedById() {
    Group first = groupRepository.save(Group.create(1L, "1조"));
    Group second = groupRepository.save(Group.create(1L, "2조"));
    groupRepository.save(Group.create(2L, "다른 기수 조"));

    assertThat(groupRepository.findByGenerationIdOrderByIdAsc(1L))
        .extracting(Group::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("조가 없는 기수는 빈 리스트를 반환한다")
  void returnsEmptyWhenNoGroups() {
    assertThat(groupRepository.findByGenerationIdOrderByIdAsc(999L)).isEmpty();
  }

  @Test
  @DisplayName("같은 기수 안에서만 이름 중복을 확인한다")
  void checksNameDuplicateWithinGenerationOnly() {
    groupRepository.save(Group.create(1L, "1조"));

    assertThat(groupRepository.existsByGenerationIdAndName(1L, "1조")).isTrue();
    assertThat(groupRepository.existsByGenerationIdAndName(2L, "1조")).isFalse();
  }

  @Test
  @DisplayName("자기 자신은 이름 중복 검사에서 제외한다")
  void excludesSelfFromNameDuplicateCheck() {
    Group group = groupRepository.save(Group.create(1L, "1조"));

    assertThat(groupRepository.existsByGenerationIdAndNameAndIdNot(1L, "1조", group.getId())).isFalse();

    Group other = groupRepository.save(Group.create(1L, "2조"));
    assertThat(groupRepository.existsByGenerationIdAndNameAndIdNot(1L, "1조", other.getId())).isTrue();
  }
}
