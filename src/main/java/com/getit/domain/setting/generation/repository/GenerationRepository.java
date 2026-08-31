package com.getit.domain.setting.generation.repository;

import com.getit.domain.setting.generation.entity.Generation;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  /**
   * {@code generationNo} 로 행을 찾으면서 잠근다. (10.2, PR #76 Copilot 리뷰 지적)
   *
   * <p>{@code GenerationAdminService} 가 활성화 로직 전체를 직렬화하는 잠금 행을 잡는 데 쓴다 —
   * 활성 기수가 하나도 없는 최초 상태에서는 {@code deactivateIfActive} 만으로 두 트랜잭션의
   * 경합을 막지 못하기 때문이다(둘 다 "활성 기수 없음"을 보고 그대로 진행해버릴 수 있다).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select g from Generation g where g.generationNo = :generationNo")
  Optional<Generation> findByGenerationNoForUpdate(@Param("generationNo") Integer generationNo);

  /**
   * 같은 잠금 행을 <b>공유</b> 모드로 잡는다. (PR #169 리뷰 지적)
   *
   * <p>"지금 활성 기수인가"를 확인하고 쓰는 쪽이 쓴다. 확인만 하고 잠그지 않으면, 확인 직후
   * 다른 트랜잭션이 새 기수를 활성화해도 이쪽은 그대로 커밋된다 — 방금 아카이브가 된 기수에
   * 자료가 새로 생기거나 지워진다(TOCTOU).
   *
   * <p>배타 잠금({@code findByGenerationNoForUpdate})을 쓰면 강의 쓰기끼리도 전부 줄을 서게
   * 된다. 공유 잠금이면 쓰기끼리는 함께 진행하고, 기수 전환만 이들이 끝날 때까지 기다린다.
   */
  @Lock(LockModeType.PESSIMISTIC_READ)
  @Query("select g from Generation g where g.generationNo = :generationNo")
  Optional<Generation> findByGenerationNoShared(@Param("generationNo") Integer generationNo);
}
