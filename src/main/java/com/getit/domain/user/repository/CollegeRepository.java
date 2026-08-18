package com.getit.domain.user.repository;

import com.getit.domain.user.entity.College;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {

  /** 2.6 목록 조회. 등록 순서(id 오름차순)로 반환한다. */
  List<College> findAllByOrderByIdAsc();
}
