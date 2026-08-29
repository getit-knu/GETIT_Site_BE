package com.getit.domain.setting.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CurriculumQueryServiceImplTest {

  @Autowired
  private CurriculumQueryService curriculumQueryService;

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Test
  @DisplayName("주어진 기수의 커리큘럼을 order 순으로 반환한다")
  void returnsCurriculumsInOrder() {
    curriculumRepository.save(Curriculum.create(1L, 2, "B", "b-subtitle"));
    curriculumRepository.save(Curriculum.create(1L, 1, "A", "a-subtitle"));
    curriculumRepository.save(Curriculum.create(2L, 1, "다른 기수", "subtitle"));

    List<CurriculumView> result = curriculumQueryService.findByGenerationId(1L);

    assertThat(result).extracting(CurriculumView::title).containsExactly("A", "B");
  }

  @Test
  @DisplayName("일치하는 커리큘럼이 없으면 빈 리스트다")
  void returnsEmptyWhenNoMatch() {
    List<CurriculumView> result = curriculumQueryService.findByGenerationId(999L);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("generationId 가 null 이면 조회 없이 빈 리스트다")
  void returnsEmptyWhenGenerationIdIsNull() {
    List<CurriculumView> result = curriculumQueryService.findByGenerationId(null);

    assertThat(result).isEmpty();
  }
}
