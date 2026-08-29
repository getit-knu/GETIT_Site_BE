package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.exception.CategoryErrorCode;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.category.service.TrackUpsert.SubCategoryNode;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CategoryBulkServiceImplTest {

  @Autowired
  private CategoryBulkService categoryBulkService;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private LectureRepository lectureRepository;

  private Track track(String name, int order) {
    return trackRepository.save(Track.create(name, order));
  }

  private SubCategory sub(String name, int order, Long trackId) {
    return subCategoryRepository.save(SubCategory.create(name, order, trackId));
  }

  private void lectureUnder(Long trackId, Long subCategoryId) {
    lectureRepository.save(Lecture.create(
        1, "강의", null, null, null, 60, true, 9L, trackId, subCategoryId, 1L));
  }

  @Test
  @DisplayName("트랙·소분류를 수정·생성·삭제하고 order 를 배열 인덱스로 재부여한다")
  void replacesTree() {
    Track sw = track("SW", 1);
    Track drop = track("삭제트랙", 2);
    SubCategory web = sub("웹기초", 1, sw.getId());
    sub("삭제소분류", 2, sw.getId());

    categoryBulkService.replaceTree(List.of(
        new TrackUpsert(sw.getId(), "SW 트랙", List.of(
            new SubCategoryNode(web.getId(), "웹 기초"),
            new SubCategoryNode(null, "타입스크립트"))),
        new TrackUpsert(null, "세미나", List.of())),
        false);

    List<Track> tracks = trackRepository.findAllByOrderByOrderAsc();
    assertThat(tracks).extracting(Track::getName).containsExactly("SW 트랙", "세미나");
    assertThat(tracks).extracting(Track::getOrder).containsExactly(1, 2);
    assertThat(trackRepository.findById(drop.getId())).isEmpty();

    List<SubCategory> subs = subCategoryRepository.findAllByTrackIdOrderByOrderAsc(sw.getId());
    assertThat(subs).extracting(SubCategory::getName).containsExactly("웹 기초", "타입스크립트");
    assertThat(subs).extracting(SubCategory::getOrder).containsExactly(1, 2);
  }

  @Test
  @DisplayName("force=false: 삭제 대상 소분류에 강의가 연결돼 있으면 CATEGORY_IN_USE 로 롤백")
  void rejectsWhenInUse() {
    Track sw = track("SW", 1);
    SubCategory web = sub("웹기초", 1, sw.getId());
    lectureUnder(sw.getId(), web.getId());

    assertThatThrownBy(() -> categoryBulkService.replaceTree(List.of(
        new TrackUpsert(sw.getId(), "SW", List.of())), false))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.CATEGORY_IN_USE);

    assertThat(subCategoryRepository.findById(web.getId())).isPresent();
  }

  @Test
  @DisplayName("force=true: 강의 연결을 해제하고 삭제한다")
  void forceDisconnectsAndDeletes() {
    Track sw = track("SW", 1);
    Track drop = track("삭제트랙", 2);
    SubCategory web = sub("웹기초", 1, sw.getId());
    lectureUnder(drop.getId(), null);

    categoryBulkService.replaceTree(List.of(
        new TrackUpsert(sw.getId(), "SW", List.of(new SubCategoryNode(web.getId(), "웹기초")))), true);

    assertThat(trackRepository.findById(drop.getId())).isEmpty();
    Lecture lecture = lectureRepository.findAll().get(0);
    assertThat(lecture.getTrackId()).isNull();
  }

  @Test
  @DisplayName("없는 트랙 id 를 수정하려 하면 TRACK_NOT_FOUND")
  void unknownTrackThrows() {
    assertThatThrownBy(() -> categoryBulkService.replaceTree(List.of(
        new TrackUpsert(999L, "없음", List.of())), false))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.TRACK_NOT_FOUND);
  }
}
