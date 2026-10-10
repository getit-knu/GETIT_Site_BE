package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.support.QueryCounter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 트리 조회가 트랙 수와 무관하게 두 번의 쿼리로 끝나는지 고정한다.
 *
 * <p>{@code CategoryQueryServiceImpl.findAllTracksWithSubCategories} 는 트랙을 한 번 읽고
 * 하위 분류를 {@code findAllByTrackIdIn} 으로 한 번 더 읽어 메모리에서 묶는다. 트랙마다
 * 하위 분류를 조회하도록 바꿔도 결과는 같고 쿼리 수만 늘어난다 — 그래서 값만 보는 테스트로는
 * 잡히지 않는다.
 *
 * <p>여기서는 절대값을 고정한다. 이 조회는 쓰기가 없어 후보 수에 비례해 늘어나야 할 쿼리가
 * 아예 없고, 트랙이 1개일 때와 여러 개일 때가 <b>같은 수</b>여야 하기 때문이다.
 */
@SpringBootTest
@Transactional
class CategoryQueryCountTest {

  private static final long EXPECTED_QUERIES = 2;
  private static final int MANY_TRACKS = 10;
  private static final int SUB_CATEGORIES_PER_TRACK = 3;

  @Autowired
  private CategoryQueryService categoryQueryService;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private EntityManager entityManager;

  private QueryCounter queryCounter;

  @BeforeEach
  void setUp() {
    queryCounter = QueryCounter.of(entityManagerFactory, entityManager);
  }

  @Test
  @DisplayName("트랙이 하나일 때 트리 조회는 두 번의 쿼리로 끝난다")
  void readsTreeInTwoQueriesForSingleTrack() {
    createTracks(1);

    long queries = queryCounter.countDuring(categoryQueryService::findAllTracksWithSubCategories);

    assertThat(queries)
        .as("트랙 조회 1회 + 하위 분류 일괄 조회 1회. 측정값 %d 회".formatted(queries))
        .isEqualTo(EXPECTED_QUERIES);
  }

  @Test
  @DisplayName("트랙이 늘어도 트리 조회의 쿼리 수는 그대로다")
  void readsTreeInTwoQueriesRegardlessOfTrackCount() {
    createTracks(MANY_TRACKS);

    long queries = queryCounter.countDuring(categoryQueryService::findAllTracksWithSubCategories);

    assertThat(queries)
        .as("""
            트랙 %d 개 · 하위 분류 %d 개인데도 쿼리는 %d 회여야 한다.
            늘어났다면 트랙마다 하위 분류를 다시 읽고 있다 (N+1). 측정값 %d 회"""
            .formatted(MANY_TRACKS, MANY_TRACKS * SUB_CATEGORIES_PER_TRACK, EXPECTED_QUERIES, queries))
        .isEqualTo(EXPECTED_QUERIES);
  }

  private void createTracks(int trackCount) {
    for (int i = 0; i < trackCount; i++) {
      Track track = trackRepository.save(Track.create("트랙-" + i, i));
      for (int j = 0; j < SUB_CATEGORIES_PER_TRACK; j++) {
        subCategoryRepository.save(SubCategory.create("하위-" + i + "-" + j, j, track.getId()));
      }
    }
  }
}
