package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.Application;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  /** 특정 기수에 낸 내 지원서 조회. (3.2) 사용자당 기수별 최대 1건이다. */
  Optional<Application> findByUserIdAndGenerationId(Long userId, Long generationId);
}
