package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.SubCategory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface SubCategoryRepository extends JpaRepository<SubCategory, Long> {

  List<SubCategory> findAllByTrackIdOrderByOrderAsc(Long trackId);
  List<SubCategory> findAllByTrackIdInOrderByTrackIdAscOrderAsc(List<Long> trackIds);
  Optional<SubCategory> findTopByTrackIdOrderByOrderDesc(Long trackId);

  /** 10.20 일괄 저장 전용. Track 잠금과 짝. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from SubCategory s order by s.trackId asc, s.order asc, s.id asc")
  List<SubCategory> findAllForUpdate();
}
