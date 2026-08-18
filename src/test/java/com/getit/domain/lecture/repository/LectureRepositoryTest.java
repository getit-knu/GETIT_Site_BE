package com.getit.domain.lecture.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class LectureRepositoryTest {

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private EntityManager entityManager;

  private Lecture save(int week, Long generationId, Long trackId, Long subCategoryId) {
    return lectureRepository.save(
        Lecture.create(week, "제목" + week, null, null, null, null, true,
            generationId, trackId, subCategoryId, 1L));
  }

  @Nested
  class FindAllByFilters {

    @Test
    @DisplayName("기수만 지정하면 해당 기수 강의를 week 오름차순으로 반환한다")
    void filtersByGenerationOnly() {
      save(2, 9L, 1L, 1L);
      save(1, 9L, 1L, 2L);
      save(1, 8L, 1L, 1L);

      assertThat(lectureRepository.findAllByFilters(9L, null, null))
          .extracting(Lecture::getWeek)
          .containsExactly(1, 2);
    }

    @Test
    @DisplayName("트랙 id 로 필터링한다")
    void filtersByTrackId() {
      save(1, 9L, 1L, 1L);
      save(2, 9L, 2L, 1L);

      assertThat(lectureRepository.findAllByFilters(9L, 1L, null))
          .extracting(Lecture::getTrackId)
          .containsExactly(1L);
    }

    @Test
    @DisplayName("소프트 삭제된 강의는 제외한다")
    void excludesSoftDeleted() {
      Lecture lecture = save(1, 9L, 1L, 1L);
      lecture.delete();
      lectureRepository.save(lecture);

      assertThat(lectureRepository.findAllByFilters(9L, null, null)).isEmpty();
    }
  }

  @Nested
  class UsageCountsForCategory {

    @Test
    @DisplayName("트랙 id 로 강의 수를 센다")
    void countsByTrackId() {
      save(1, 9L, 1L, 1L);
      save(2, 9L, 1L, 2L);
      save(3, 9L, 2L, null);

      assertThat(lectureRepository.countByTrackIdAndDeletedAtIsNull(1L)).isEqualTo(2);
    }

    @Test
    @DisplayName("소분류 id 배치로 강의 수를 그룹핑해 센다")
    void countsBySubCategoryIdsGrouped() {
      save(1, 9L, 1L, 1L);
      save(2, 9L, 1L, 1L);
      save(3, 9L, 1L, 2L);

      List<LectureRepository.SubCategoryLectureCount> counts =
          lectureRepository.countBySubCategoryIdsGrouped(List.of(1L, 2L));

      assertThat(counts)
          .extracting(LectureRepository.SubCategoryLectureCount::getSubCategoryId,
              LectureRepository.SubCategoryLectureCount::getCount)
          .containsExactlyInAnyOrder(tuple(1L, 2L), tuple(2L, 1L));
    }

    @Test
    @DisplayName("소분류 id 목록에 속한 강의의 소분류 연결을 해제한다")
    void disconnectsBySubCategoryIds() {
      Lecture lecture = save(1, 9L, 1L, 1L);

      lectureRepository.disconnectBySubCategoryIds(List.of(1L));
      entityManager.flush();
      entityManager.clear();

      assertThat(lectureRepository.findById(lecture.getId()))
          .get()
          .extracting(Lecture::getSubCategoryId)
          .isNull();
    }
  }
}
