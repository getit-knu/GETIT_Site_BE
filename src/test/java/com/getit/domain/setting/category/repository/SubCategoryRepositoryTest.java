package com.getit.domain.setting.category.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class SubCategoryRepositoryTest {

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Test
  @DisplayName("소속 트랙 기준으로 order 오름차순 조회한다")
  void findsAllByTrackIdOrderedByOrder() {
    Track track = trackRepository.save(Track.create("SW", 1));
    Track otherTrack = trackRepository.save(Track.create("창업", 2));
    subCategoryRepository.save(SubCategory.create("React.js", 2, track.getId()));
    subCategoryRepository.save(SubCategory.create("웹기초", 1, track.getId()));
    subCategoryRepository.save(SubCategory.create("Figma", 1, otherTrack.getId()));

    assertThat(subCategoryRepository.findAllByTrackIdOrderByOrderAsc(track.getId()))
        .extracting(SubCategory::getName)
        .containsExactly("웹기초", "React.js");
  }

  @Test
  @DisplayName("소속 트랙 기준으로 가장 큰 order 를 가진 소분류 하나를 반환한다")
  void findsTopByTrackIdOrderByOrder() {
    Track track = trackRepository.save(Track.create("SW", 1));
    subCategoryRepository.save(SubCategory.create("웹기초", 1, track.getId()));
    subCategoryRepository.save(SubCategory.create("React.js", 3, track.getId()));
    subCategoryRepository.save(SubCategory.create("Node.js", 2, track.getId()));

    assertThat(subCategoryRepository.findTopByTrackIdOrderByOrderDesc(track.getId()))
        .isPresent()
        .get()
        .extracting(SubCategory::getName)
        .isEqualTo("React.js");
  }

  @Test
  @DisplayName("해당 트랙에 소분류가 없으면 빈 Optional 을 반환한다")
  void returnsEmptyWhenNoSubCategory() {
    Track track = trackRepository.save(Track.create("SW", 1));

    assertThat(subCategoryRepository.findTopByTrackIdOrderByOrderDesc(track.getId())).isEmpty();
  }
}
