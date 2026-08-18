package com.getit.domain.setting.category.service;

import com.getit.domain.lecture.service.CategoryUsageChecker;
import com.getit.domain.setting.category.dto.CategoryTreeResult.SubCategoryNode;
import com.getit.domain.setting.category.dto.CategoryTreeResult.TrackNode;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.exception.CategoryErrorCode;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.global.exception.BusinessException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

  private final TrackRepository trackRepository;
  private final SubCategoryRepository subCategoryRepository;
  private final CategoryUsageChecker categoryUsageChecker;

  @Transactional
  public Track createTrack(String name) {
    return trackRepository.save(Track.create(name, nextTrackOrder()));
  }

  @Transactional
  public Track updateTrack(Long id, String name, Integer order) {
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND));

    Integer resolvedOrder = order != null ? order : track.getOrder();
    track.update(name, resolvedOrder);
    return track;
  }

  @Transactional
  public void deleteTrack(Long id, boolean force) {
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND));

    List<SubCategory> subCategories = subCategoryRepository.findAllByTrackIdOrderByOrderAsc(id);

    if (force) {
      List<Long> subCategoryIds = subCategories.stream().map(SubCategory::getId).toList();
      categoryUsageChecker.disconnectLecturesBySubCategoryIds(subCategoryIds);
    } else if (isTrackInUse(id, subCategories)) {
      throw new BusinessException(CategoryErrorCode.CATEGORY_IN_USE);
    }

    subCategoryRepository.deleteAll(subCategories);
    trackRepository.delete(track);
  }

  private boolean isTrackInUse(Long trackId, List<SubCategory> subCategories) {
    if (categoryUsageChecker.countLecturesByTrackId(trackId) > 0) {
      return true;
    }
    List<Long> subCategoryIds = subCategories.stream().map(SubCategory::getId).toList();
    return categoryUsageChecker.countLecturesBySubCategoryIds(subCategoryIds).values().stream()
        .anyMatch(count -> count > 0);
  }

  private Integer nextTrackOrder() {
    return trackRepository.findTopByOrderByOrderDesc()
        .map(Track::getOrder)
        .orElse(0) + 1;
  }

  @Transactional
  public SubCategory createSubCategory(Long trackId, String name) {
    if (!trackRepository.existsById(trackId)) {
      throw new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND);
    }

    return subCategoryRepository.save(SubCategory.create(name, nextSubCategoryOrder(trackId), trackId));
  }

  @Transactional
  public SubCategory updateSubCategory(Long id, String name, Integer order) {
    SubCategory subCategory = subCategoryRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.SUBCATEGORY_NOT_FOUND));

    Integer resolvedOrder = order != null ? order : subCategory.getOrder();
    subCategory.update(name, resolvedOrder);
    return subCategory;
  }

  @Transactional
  public void deleteSubCategory(Long id, boolean force) {
    SubCategory subCategory = subCategoryRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.SUBCATEGORY_NOT_FOUND));

    if (force) {
      categoryUsageChecker.disconnectLecturesBySubCategoryIds(List.of(id));
    } else if (categoryUsageChecker.countLecturesBySubCategoryId(id) > 0) {
      throw new BusinessException(CategoryErrorCode.CATEGORY_IN_USE);
    }

    subCategoryRepository.delete(subCategory);
  }

  private Integer nextSubCategoryOrder(Long trackId) {
    return subCategoryRepository.findTopByTrackIdOrderByOrderDesc(trackId)
        .map(SubCategory::getOrder)
        .orElse(0) + 1;
  }

  public List<TrackNode> getCategoryTree() {
    List<Track> tracks = trackRepository.findAllByOrderByOrderAsc();
    List<Long> trackIds = tracks.stream().map(Track::getId).toList();

    List<SubCategory> subCategories = subCategoryRepository.findAllByTrackIdInOrderByTrackIdAscOrderAsc(trackIds);
    Map<Long, List<SubCategory>> subCategoriesByTrackId = subCategories.stream()
        .collect(Collectors.groupingBy(SubCategory::getTrackId, LinkedHashMap::new, Collectors.toList()));

    List<Long> subCategoryIds = subCategories.stream().map(SubCategory::getId).toList();
    Map<Long, Long> lectureCounts = categoryUsageChecker.countLecturesBySubCategoryIds(subCategoryIds);

    return tracks.stream()
        .map(track -> TrackNode.of(track, subCategoryNodesOf(track.getId(), subCategoriesByTrackId, lectureCounts)))
        .toList();
  }

  private List<SubCategoryNode> subCategoryNodesOf(
      Long trackId, Map<Long, List<SubCategory>> subCategoriesByTrackId, Map<Long, Long> lectureCounts) {
    return subCategoriesByTrackId.getOrDefault(trackId, List.of()).stream()
        .map(subCategory -> SubCategoryNode.of(subCategory, lectureCounts.getOrDefault(subCategory.getId(), 0L)))
        .toList();
  }
}
