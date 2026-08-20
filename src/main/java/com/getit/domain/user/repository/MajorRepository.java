package com.getit.domain.user.repository;

import com.getit.domain.user.entity.Major;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MajorRepository extends JpaRepository<Major, Long> {

  /** 2.7 목록 조회. 등록 순서(id 오름차순)로 반환한다. */
  List<Major> findAllByOrderByIdAsc();

  /** 2.7 collegeId 로 필터링한 목록 조회. */
  List<Major> findByCollegeIdOrderByIdAsc(Long collegeId);
}
