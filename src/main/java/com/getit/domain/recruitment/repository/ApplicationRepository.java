package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  /** 특정 기수에 낸 내 지원서 조회. (3.2) 사용자당 기수별 최대 1건이다. */
  Optional<Application> findByUserIdAndGenerationId(Long userId, Long generationId);

  /** 관리자 지원자 목록(7.1) - status 필터가 있을 때 조회한다. */
  Page<Application> findByGenerationIdAndStatus(Long generationId, ApplicationStatus status, Pageable pageable);

  /**
   * 관리자 지원자 목록(7.1) - status 필터가 없을 때 조회한다. 임시 저장만 하고 제출하지 않은
   * (DRAFT) 지원서는 심사 대상이 아니므로 제외한다.
   */
  Page<Application> findByGenerationIdAndStatusNot(Long generationId, ApplicationStatus status, Pageable pageable);
}
