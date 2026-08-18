package com.getit.domain.setting.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.getit.domain.lecture.service.CategoryUsageChecker;
import com.getit.domain.setting.category.dto.CategoryTreeResult.TrackNode;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.exception.CategoryErrorCode;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

  @Mock
  private TrackRepository trackRepository;

  @Mock
  private SubCategoryRepository subCategoryRepository;

  @Mock
  private CategoryUsageChecker categoryUsageChecker;

  @InjectMocks
  private CategoryService categoryService;

  private Track track() {
    Track track = Track.create("SW", 1);
    ReflectionTestUtils.setField(track, "id", 100L);
    return track;
  }

  private SubCategory subCategory(Long trackId) {
    SubCategory subCategory = SubCategory.create("웹기초", 1, trackId);
    ReflectionTestUtils.setField(subCategory, "id", 10L);
    return subCategory;
  }

  @Nested
  @DisplayName("createTrack")
  class CreateTrack {

    @Test
    @DisplayName("기존 트랙 없음: order 1로 생성")
    void assignsFirstOrderWhenNoTrackExists() {
      when(trackRepository.findTopByOrderByOrderDesc()).thenReturn(Optional.empty());
      when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      Track saved = categoryService.createTrack("SW");

      assertThat(saved.getOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("기존 트랙 있음: 최댓값+1로 생성")
    void assignsNextOrderWhenTracksExist() {
      Track existing = Track.create("창업", 3);
      when(trackRepository.findTopByOrderByOrderDesc()).thenReturn(Optional.of(existing));
      when(trackRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      Track saved = categoryService.createTrack("SW");

      assertThat(saved.getOrder()).isEqualTo(4);
    }
  }

  @Nested
  @DisplayName("updateTrack")
  class UpdateTrack {

    @Test
    @DisplayName("존재하지 않는 트랙: 예외 발생")
    void throwsWhenNotFound() {
      when(trackRepository.findById(1L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> categoryService.updateTrack(1L, "SW", 1))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.TRACK_NOT_FOUND);
    }

    @Test
    @DisplayName("order 생략 시 기존 순서 유지")
    void keepsExistingOrderWhenOmitted() {
      Track track = track();
      when(trackRepository.findById(1L)).thenReturn(Optional.of(track));

      Track updated = categoryService.updateTrack(1L, "SW 개편", null);

      assertThat(updated.getName()).isEqualTo("SW 개편");
      assertThat(updated.getOrder()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("deleteTrack")
  class DeleteTrack {

    @Test
    @DisplayName("연결된 강의 없음: 정상 삭제")
    void deletesWhenNotInUse() {
      Track track = track();
      when(trackRepository.findById(1L)).thenReturn(Optional.of(track));
      when(subCategoryRepository.findAllByTrackIdOrderByOrderAsc(1L)).thenReturn(List.of());
      when(categoryUsageChecker.countLecturesByTrackId(1L)).thenReturn(0L);
      when(categoryUsageChecker.countLecturesBySubCategoryIds(List.of())).thenReturn(Map.of());

      categoryService.deleteTrack(1L, false);

      verify(trackRepository).delete(track);
    }

    @Test
    @DisplayName("트랙 자신에 연결된 강의 있음: 예외 발생")
    void throwsWhenTrackItselfInUse() {
      when(trackRepository.findById(1L)).thenReturn(Optional.of(track()));
      when(subCategoryRepository.findAllByTrackIdOrderByOrderAsc(1L)).thenReturn(List.of());
      when(categoryUsageChecker.countLecturesByTrackId(1L)).thenReturn(1L);

      assertThatThrownBy(() -> categoryService.deleteTrack(1L, false))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.CATEGORY_IN_USE);

      verify(trackRepository, never()).delete(any());
    }

    @Test
    @DisplayName("하위 소분류에 연결된 강의 있음: 예외 발생")
    void throwsWhenSubCategoryInUse() {
      SubCategory subCategory = subCategory(1L);
      when(trackRepository.findById(1L)).thenReturn(Optional.of(track()));
      when(subCategoryRepository.findAllByTrackIdOrderByOrderAsc(1L)).thenReturn(List.of(subCategory));
      when(categoryUsageChecker.countLecturesByTrackId(1L)).thenReturn(0L);
      when(categoryUsageChecker.countLecturesBySubCategoryIds(List.of(subCategory.getId())))
          .thenReturn(Map.of(subCategory.getId(), 2L));

      assertThatThrownBy(() -> categoryService.deleteTrack(1L, false))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.CATEGORY_IN_USE);
    }

    @Test
    @DisplayName("force=true: 사용 중이어도 하위 소분류까지 함께 삭제")
    void forceDeletesEvenWhenInUse() {
      Track track = track();
      SubCategory subCategory = subCategory(1L);
      when(trackRepository.findById(1L)).thenReturn(Optional.of(track));
      when(subCategoryRepository.findAllByTrackIdOrderByOrderAsc(1L)).thenReturn(List.of(subCategory));

      categoryService.deleteTrack(1L, true);

      verify(categoryUsageChecker).disconnectLecturesBySubCategoryIds(List.of(subCategory.getId()));
      verify(subCategoryRepository).deleteAll(List.of(subCategory));
      verify(trackRepository).delete(track);
      verify(categoryUsageChecker, never()).countLecturesByTrackId(anyLong());
      verify(categoryUsageChecker, never()).countLecturesBySubCategoryIds(any());
    }
  }

  @Nested
  @DisplayName("createSubCategory")
  class CreateSubCategory {

    @Test
    @DisplayName("존재하지 않는 트랙: 예외 발생")
    void throwsWhenTrackNotFound() {
      when(trackRepository.existsById(1L)).thenReturn(false);

      assertThatThrownBy(() -> categoryService.createSubCategory(1L, "웹기초"))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.TRACK_NOT_FOUND);
    }

    @Test
    @DisplayName("기존 소분류 없음: order 1로 생성")
    void assignsFirstOrderWhenNoSubCategoryExists() {
      when(trackRepository.existsById(1L)).thenReturn(true);
      when(subCategoryRepository.findTopByTrackIdOrderByOrderDesc(1L)).thenReturn(Optional.empty());
      when(subCategoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

      SubCategory saved = categoryService.createSubCategory(1L, "웹기초");

      assertThat(saved.getOrder()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("deleteSubCategory")
  class DeleteSubCategory {

    @Test
    @DisplayName("연결된 강의 있음: 예외 발생")
    void throwsWhenInUse() {
      SubCategory subCategory = subCategory(1L);
      when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));
      when(categoryUsageChecker.countLecturesBySubCategoryId(1L)).thenReturn(1L);

      assertThatThrownBy(() -> categoryService.deleteSubCategory(1L, false))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CategoryErrorCode.CATEGORY_IN_USE);
    }

    @Test
    @DisplayName("force=true: 사용 중이어도 삭제")
    void forceDeletesEvenWhenInUse() {
      SubCategory subCategory = subCategory(1L);
      when(subCategoryRepository.findById(1L)).thenReturn(Optional.of(subCategory));

      categoryService.deleteSubCategory(1L, true);

      verify(categoryUsageChecker).disconnectLecturesBySubCategoryIds(List.of(1L));
      verify(subCategoryRepository).delete(subCategory);
      verify(categoryUsageChecker, never()).countLecturesBySubCategoryId(anyLong());
    }
  }

  @Nested
  @DisplayName("getCategoryTree")
  class GetCategoryTree {

    @Test
    @DisplayName("트랙-소분류 계층 구조와 lectureCount를 반환한다")
    void returnsTreeWithLectureCount() {
      Track track = track();
      SubCategory subCategory = subCategory(track.getId());
      when(trackRepository.findAllByOrderByOrderAsc()).thenReturn(List.of(track));
      when(subCategoryRepository.findAllByTrackIdInOrderByTrackIdAscOrderAsc(List.of(track.getId())))
          .thenReturn(List.of(subCategory));
      when(categoryUsageChecker.countLecturesBySubCategoryIds(List.of(subCategory.getId())))
          .thenReturn(Map.of(subCategory.getId(), 3L));

      List<TrackNode> tree = categoryService.getCategoryTree();

      assertThat(tree).hasSize(1);
      assertThat(tree.get(0).subCategories()).hasSize(1);
      assertThat(tree.get(0).subCategories().get(0).lectureCount()).isEqualTo(3L);
    }
  }
}
