package com.getit.domain.setting.category.service;

import com.getit.domain.setting.category.dto.CategorySummary;
import java.util.List;
import java.util.Optional;

public interface CategoryQueryService {

  boolean existsTrack(Long trackId);

  Optional<Long> findTrackIdOfSubCategory(Long subCategoryId);

  List<CategorySummary> findAllTracksWithSubCategories();
}
