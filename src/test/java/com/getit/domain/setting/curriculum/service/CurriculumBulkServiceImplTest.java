package com.getit.domain.setting.curriculum.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.exception.CurriculumErrorCode;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CurriculumBulkServiceImplTest {

  private static final long GENERATION_ID = 1L;

  @Autowired
  private CurriculumBulkService curriculumBulkService;

  @Autowired
  private CurriculumRepository curriculumRepository;

  private Curriculum saved(int order, String title) {
    return curriculumRepository.save(Curriculum.create(GENERATION_ID, order, title, "부제"));
  }

  @Test
  @DisplayName("id 있으면 수정 · 없으면 생성 · 리스트에 없는 기존 행은 삭제 · order 는 배열 인덱스")
  void replacesWholeSet() {
    Curriculum keep = saved(1, "유지");
    Curriculum gone = saved(2, "삭제될것");

    curriculumBulkService.replaceAll(GENERATION_ID, List.of(
        new CurriculumUpsert(null, "신규", "신규부제"),
        new CurriculumUpsert(keep.getId(), "수정됨", "수정부제")));

    List<Curriculum> result = curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(GENERATION_ID);
    assertThat(result).extracting(Curriculum::getTitle).containsExactly("신규", "수정됨");
    assertThat(result).extracting(Curriculum::getOrder).containsExactly(1, 2);
    assertThat(curriculumRepository.findById(gone.getId())).isEmpty();
  }

  @Test
  @DisplayName("수정은 title·subtitle 을 전부 덮어쓴다")
  void updateOverwritesAllFields() {
    Curriculum curriculum = saved(1, "원본");

    curriculumBulkService.replaceAll(GENERATION_ID, List.of(
        new CurriculumUpsert(curriculum.getId(), "바뀐제목", "바뀐부제")));

    Curriculum updated = curriculumRepository.findById(curriculum.getId()).orElseThrow();
    assertThat(updated.getTitle()).isEqualTo("바뀐제목");
    assertThat(updated.getSubtitle()).isEqualTo("바뀐부제");
  }

  @Test
  @DisplayName("생성·삭제 없이 순서만 바꿔도 order 가 배열 인덱스로 재부여된다")
  void reorderOnly() {
    Curriculum a = saved(1, "A");
    Curriculum b = saved(2, "B");

    curriculumBulkService.replaceAll(GENERATION_ID, List.of(
        new CurriculumUpsert(b.getId(), "B", "부제"),
        new CurriculumUpsert(a.getId(), "A", "부제")));

    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(GENERATION_ID))
        .extracting(Curriculum::getTitle).containsExactly("B", "A");
    assertThat(curriculumRepository.findById(b.getId()).orElseThrow().getOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("빈 리스트를 넘기면 전부 삭제된다")
  void emptyListDeletesAll() {
    saved(1, "A");
    saved(2, "B");

    curriculumBulkService.replaceAll(GENERATION_ID, List.of());

    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(GENERATION_ID)).isEmpty();
  }

  @Test
  @DisplayName("존재하지 않는 id 가 섞이면 예외가 발생하고 트랜잭션이 롤백된다")
  void throwsWhenIdNotFound() {
    saved(1, "유지될것");

    assertThatThrownBy(() -> curriculumBulkService.replaceAll(GENERATION_ID, List.of(
        new CurriculumUpsert(999L, "없는id", "부제"))))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
  }

  @Test
  @DisplayName("다른 기수의 커리큘럼은 건드리지 않는다")
  void doesNotTouchOtherGeneration() {
    Curriculum otherGeneration = curriculumRepository.save(Curriculum.create(2L, 1, "다른 기수", "부제"));

    curriculumBulkService.replaceAll(GENERATION_ID, List.of());

    assertThat(curriculumRepository.findById(otherGeneration.getId())).isPresent();
  }
}
