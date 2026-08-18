package com.getit.domain.setting.category.service;

import com.getit.domain.setting.category.dto.CategorySummary;
import com.getit.domain.setting.category.dto.CategorySummary.SubCategoryBrief;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryQueryServiceImpl implements CategoryQueryService {

  private final TrackRepository trackRepository;
  private final SubCategoryRepository subCategoryRepository;

  @Override
  public boolean existsTrack(Long trackId) {
    return trackRepository.existsById(trackId);
  }

  @Override
  public Optional<Long> findTrackIdOfSubCategory(Long subCategoryId) {
    return subCategoryRepository.findById(subCategoryId).map(SubCategory::getTrackId);
  }

  @Override
  public List<CategorySummary> findAllTracksWithSubCategories() {
    List<Track> tracks = trackRepository.findAllByOrderByOrderAsc();
    List<Long> trackIds = tracks.stream().map(Track::getId).toList();

    Map<Long, List<SubCategory>> subCategoriesByTrackId =
        subCategoryRepository.findAllByTrackIdInOrderByTrackIdAscOrderAsc(trackIds).stream()
            .collect(Collectors.groupingBy(SubCategory::getTrackId, LinkedHashMap::new, Collectors.toList()));

    return tracks.stream()
        .map(track -> new CategorySummary(
            track.getId(),
            track.getName(),
            subCategoriesByTrackId.getOrDefault(track.getId(), List.of()).stream()
                .map(subCategory -> new SubCategoryBrief(subCategory.getId(), subCategory.getName()))
                .toList()))
        .toList();
  }
}
