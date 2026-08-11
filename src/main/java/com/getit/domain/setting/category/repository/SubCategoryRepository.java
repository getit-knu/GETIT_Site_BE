package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.SubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

  long countByTrackId(Long trackId);
}
