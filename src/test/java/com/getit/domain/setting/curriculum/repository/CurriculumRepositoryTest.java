package com.getit.domain.setting.curriculum.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CurriculumRepositoryTest {

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Test
  @DisplayName("기수의 커리큘럼을 order 오름차순으로 조회한다")
  void findsByGenerationIdOrderByOrderAscIdAsc() {
    curriculumRepository.save(Curriculum.create(9L, 2, "웹 개발", "React, Node.js"));
    curriculumRepository.save(Curriculum.create(9L, 1, "Python & 데이터 분석", "Python 기초"));
    curriculumRepository.save(Curriculum.create(8L, 1, "지난 기수 커리큘럼", "지난 기수"));

    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(9L))
        .extracting(Curriculum::getTitle)
        .containsExactly("Python & 데이터 분석", "웹 개발");
  }

  @Test
  @DisplayName("order 가 같으면 id 오름차순으로 정렬한다")
  void tieBreaksById() {
    Curriculum first = curriculumRepository.save(Curriculum.create(9L, 1, "A", "A"));
    Curriculum second = curriculumRepository.save(Curriculum.create(9L, 1, "B", "B"));

    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(9L))
        .extracting(Curriculum::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("id 와 소속 기수가 둘 다 일치할 때만 조회한다")
  void findsByIdAndGenerationIdOnlyWhenBothMatch() {
    Curriculum saved = curriculumRepository.save(Curriculum.create(9L, 1, "Python & 데이터 분석", "Python 기초"));

    assertThat(curriculumRepository.findByIdAndGenerationId(saved.getId(), 9L)).isPresent();
    assertThat(curriculumRepository.findByIdAndGenerationId(saved.getId(), 8L)).isEmpty();
  }
}
