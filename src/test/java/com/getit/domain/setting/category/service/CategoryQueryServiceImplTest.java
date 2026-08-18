package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import com.getit.domain.setting.category.dto.CategorySummary;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 소비하는 트랙·소분류 조회 계약. (이슈 #25) */
@SpringBootTest
@Transactional
class CategoryQueryServiceImplTest {

  @Autowired
  private CategoryQueryService categoryQueryService;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Test
  @DisplayName("존재하는 트랙 id면 true 를 반환한다")
  void existsTrackReturnsTrue() {
    Track track = trackRepository.save(Track.create("SW", 1));

    assertThat(categoryQueryService.existsTrack(track.getId())).isTrue();
  }

  @Test
  @DisplayName("없는 트랙 id면 false 를 반환한다")
  void existsTrackReturnsFalseForUnknownId() {
    assertThat(categoryQueryService.existsTrack(999_999L)).isFalse();
  }

  @Test
  @DisplayName("소분류의 소속 트랙 id 를 반환한다")
  void findsTrackIdOfSubCategory() {
    Track track = trackRepository.save(Track.create("SW", 1));
    SubCategory subCategory = subCategoryRepository.save(SubCategory.create("React.js", 1, track.getId()));

    assertThat(categoryQueryService.findTrackIdOfSubCategory(subCategory.getId())).contains(track.getId());
  }

  @Test
  @DisplayName("없는 소분류 id면 빈 Optional 을 반환한다")
  void returnsEmptyForUnknownSubCategory() {
    assertThat(categoryQueryService.findTrackIdOfSubCategory(999_999L)).isEmpty();
  }

  @Test
  @DisplayName("트랙 order 순으로, 각 트랙의 소분류도 order 순으로 묶어 반환한다")
  void findsAllTracksWithSubCategories() {
    Track sw = trackRepository.save(Track.create("SW", 2));
    Track startup = trackRepository.save(Track.create("창업", 1));
    subCategoryRepository.save(SubCategory.create("React.js", 2, sw.getId()));
    subCategoryRepository.save(SubCategory.create("웹기초", 1, sw.getId()));

    List<CategorySummary> result = categoryQueryService.findAllTracksWithSubCategories();

    assertThat(result).extracting("id", "name")
        .containsExactly(tuple(startup.getId(), "창업"), tuple(sw.getId(), "SW"));
    assertThat(result.get(1).subCategories()).extracting("name").containsExactly("웹기초", "React.js");
    assertThat(result.get(0).subCategories()).isEmpty();
  }
}
