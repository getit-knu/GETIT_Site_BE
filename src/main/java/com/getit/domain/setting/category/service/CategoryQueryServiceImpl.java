package com.getit.domain.setting.category.service;

import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import java.util.Optional;
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
}
