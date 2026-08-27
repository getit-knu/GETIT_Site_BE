package com.getit.domain.setting.generation.repository;

import com.getit.domain.setting.generation.entity.Generation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationRepository extends JpaRepository<Generation, Long> {

  /** 현재 진행 기수. 항상 0건 또는 1건이다. (설계 명세서 4.5) */
  Optional<Generation> findByIsActiveTrue();

  Optional<Generation> findByGenerationNo(Integer generationNo);

  boolean existsByGenerationNo(Integer generationNo);

  /**
   * 여전히 활성 상태인 행만 원자적으로 비활성화한다. (10.2)
   *
   * <p>"활성 기수인지 확인 후 비활성화"를 자바에서 두 단계로 하면, 그 사이에 다른 요청이 같은
   * 기수를 먼저 비활성화(또는 다른 기수를 활성화)해버릴 수 있다 — 활성 기수 단일성은 DB 제약으로
   * 표현할 수 없어(설계 명세서 4.5) 서비스가 보장해야 하기 때문이다. 조건과 반영을 UPDATE 하나로
   * 묶으면 DB 행 잠금으로 직렬화되므로, 반영된 행 수가 0이면 그 사이 다른 요청이 먼저 비활성화한
   * 것이다({@code UserRepository.assignToGroupIfUnassigned} 와 동일한 패턴).
   */
  @Modifying(clearAutomatically = true)
  @Query("update Generation g set g.isActive = false where g.id = :id and g.isActive = true")
  int deactivateIfActive(@Param("id") Long id);
}
