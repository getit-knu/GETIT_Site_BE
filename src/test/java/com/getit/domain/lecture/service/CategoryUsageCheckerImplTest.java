package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인(category)이 소비하는 강의 사용 여부 조회 계약. (이슈 #25, PR #31 리뷰) */
@SpringBootTest
@Transactional
class CategoryUsageCheckerImplTest {

  @Autowired
  private CategoryUsageChecker categoryUsageChecker;

  @Autowired
  private LectureRepository lectureRepository;

  private Lecture save(Long trackId, Long subCategoryId) {
    return lectureRepository.save(
        Lecture.create(1, "제목", null, null, null, null, true, 9L, trackId, subCategoryId, 1L));
  }

  @Test
  @DisplayName("트랙 id 로 강의 수를 센다")
  void countsLecturesByTrackId() {
    save(1L, 1L);
    save(1L, 2L);
    save(2L, null);

    assertThat(categoryUsageChecker.countLecturesByTrackId(1L)).isEqualTo(2);
  }

  @Test
  @DisplayName("소분류 id 로 강의 수를 센다")
  void countsLecturesBySubCategoryId() {
    save(1L, 1L);

    assertThat(categoryUsageChecker.countLecturesBySubCategoryId(1L)).isEqualTo(1);
  }

  @Test
  @DisplayName("배치 조회: 강의가 없는 id 는 0 을 반환한다")
  void batchCountsFillZeroForUnusedId() {
    save(1L, 1L);

    Map<Long, Long> counts = categoryUsageChecker.countLecturesBySubCategoryIds(List.of(1L, 2L));

    assertThat(counts).isEqualTo(Map.of(1L, 1L, 2L, 0L));
  }

  @Test
  @DisplayName("소분류 연결을 해제하면 트랙은 유지된 채 소분류만 비워진다")
  void disconnectsSubCategoryOnly() {
    Lecture lecture = save(1L, 1L);

    categoryUsageChecker.disconnectLecturesBySubCategoryIds(List.of(1L));

    Lecture reloaded = lectureRepository.findById(lecture.getId()).orElseThrow();
    assertThat(reloaded.getSubCategoryId()).isNull();
    assertThat(reloaded.getTrackId()).isEqualTo(1L);
  }
}
