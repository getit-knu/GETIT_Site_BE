package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.SubCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

  List<SubCategory> findAllByTrackIdOrderByOrderAsc(Long trackId);
  List<SubCategory> findAllByTrackIdInOrderByTrackIdAscOrderAsc(List<Long> trackIds);
  Optional<SubCategory> findTopByTrackIdOrderByOrderDesc(Long trackId);
}
