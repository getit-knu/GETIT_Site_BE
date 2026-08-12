package com.getit.domain.setting.category.service;

import com.getit.domain.setting.category.dto.CategoryTreeResult.SubCategoryNode;
import com.getit.domain.setting.category.dto.CategoryTreeResult.TrackNode;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.exception.CategoryErrorCode;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
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
  public Track createTrack(String name, Integer order) {
    Integer resolvedOrder = order != null ? order : nextTrackOrder();
    return trackRepository.save(Track.create(name, resolvedOrder));
  }

  @Transactional
  public Track updateTrack(Long id, String name, Integer order) {
    Track track = trackRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND));

    Integer resolvedOrder = order != null ? order : track.getOrder();
    track.update(name, resolvedOrder);
    return track;
  }

  private Integer nextTrackOrder() { return (int) trackRepository.count() + 1; }

  @Transactional
  public SubCategory createSubCategory(Long trackId, String name, Integer order) {
    if (!trackRepository.existsById(trackId)) {
      throw new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND);
    }

    Integer resolvedOrder = order != null ? order : nextSubCategoryOrder(trackId);
    return subCategoryRepository.save(SubCategory.create(name, resolvedOrder, trackId));
  }

  @Transactional
  public SubCategory updateSubCategory(Long id, String name, Integer order) {
    SubCategory subCategory = subCategoryRepository.findById(id)
        .orElseThrow(() -> new BusinessException(CategoryErrorCode.SUBCATEGORY_NOT_FOUND));

    Integer resolvedOrder = order != null ? order : subCategory.getOrder();
    subCategory.update(name, resolvedOrder);
    return subCategory;
  }

  private Integer nextSubCategoryOrder(Long trackId) { return (int) subCategoryRepository.countByTrackId(trackId) + 1; }

  public List<TrackNode> getCategoryTree() {
    return trackRepository.findAllByOrderByOrderAsc().stream()
        .map(track -> TrackNode.of(track, subCategoryNodesOf(track.getId())))
        .toList();
  }

  private List<SubCategoryNode> subCategoryNodesOf(Long trackId) {
    return subCategoryRepository.findAllByTrackIdOrderByOrderAsc(trackId).stream()
        .map(subCategory -> SubCategoryNode.of(
            subCategory, categoryUsageChecker.countLecturesBySubCategoryId(subCategory.getId())))
        .toList();
  }
}
