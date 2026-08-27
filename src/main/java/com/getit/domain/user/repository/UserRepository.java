package com.getit.domain.user.repository;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  /** OAuth 로그인 시 기존 사용자 조회에 쓴다. */
  Optional<User> findByProviderId(String providerId);

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  /** 특정 기수의 활성 부원. (8.6 제출 현황 모집단, #30) */
  List<User> findByRoleAndStatusAndGenerationNo(Role role, UserStatus status, Integer generationNo);

  /**
   * 특정 기수의 활성 사용자 전체. 조 배정 여부와 무관하다.
   * (9.6 조 관리 보드 — 조별 명단과 미배정 명단을 이 결과 하나를 groupId 로 나눠서 만든다)
   */
  List<User> findByGenerationNoAndStatus(Integer generationNo, UserStatus status);

  /** 조에 속한 활성 사용자. 조원 수 표기(9.6 memberCount · 9.8 응답)에 쓴다. */
  List<User> findByGroupIdAndStatus(Long groupId, UserStatus status);

  /**
   * 조에 속한 사용자 전체(상태 무관). 조 삭제 시 배정 해제 대상을 찾는 데 쓴다. (9.9)
   * 탈퇴한 사용자도 배정 정보가 남아있으면 지워야 나중에 복구됐을 때 없어진 조를 가리키지 않는다.
   */
  List<User> findByGroupId(Long groupId);

  /**
   * 관리자 사용자 목록(9.1). keyword · role · generationNo · groupId 모두 선택 필터다 —
   * 각 조건은 파라미터가 null 이면(= 필터 안 씀) 통과시키는 방식으로 하나의 쿼리에 합쳤다.
   *
   * <p>groupId 필터는 세 가지 경우가 있다: 필터 없음 / 특정 조 / 미배정("none")뿐이다.
   * {@code unassignedOnly} 로 세 번째 경우를 표현한다 — {@code groupId} 하나만으로는 "필터
   * 없음(null)"과 "미배정 조회(값이 있어야 하는데 값 자체가 없는 조건)"를 구분할 수 없기 때문이다.
   * (9.6 조회에서 groupId=null 로 findByGenerationNoAndStatus 를 쓰는 것과 달리, 여기는 필터
   * 조합이 많아 derived method 로 나누면 경우의 수가 너무 많아진다)
   */
  @Query("select u from User u "
      + "where (:keyword is null or lower(u.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(u.email) like lower(concat('%', :keyword, '%'))) "
      + "and (:role is null or u.role = :role) "
      + "and (:generationNo is null or u.generationNo = :generationNo) "
      + "and (:unassignedOnly = false or u.groupId is null) "
      + "and (:unassignedOnly = true or :groupId is null or u.groupId = :groupId)")
  Page<User> searchUsers(
      @Param("keyword") String keyword,
      @Param("role") Role role,
      @Param("generationNo") Integer generationNo,
      @Param("groupId") Long groupId,
      @Param("unassignedOnly") boolean unassignedOnly,
      Pageable pageable);

  /**
   * 미배정 상태(groupId IS NULL)인 행만 원자적으로 조에 배정한다. (9.10)
   *
   * <p>"미배정인지 확인 후 배정"을 자바에서 두 단계로 하면, 그 사이에 다른 요청이 같은 사용자를
   * 먼저 배정해버릴 수 있다 (PR #60 Copilot 리뷰 지적). UPDATE 문 하나로 조건과 반영을 묶으면
   * DB 행 잠금으로 직렬화되므로, 반환된 반영 행 수가 요청한 인원 수보다 적으면 경합이 있었다는
   * 뜻이다 — 그때는 호출자가 트랜잭션을 롤백시켜야 한다 ({@code ApplicationRepository
   * .updateStatusIfCurrentStatus} 와 동일한 패턴, PR #52 Copilot 리뷰 지적에서 확립됨).
   */
  @Modifying(clearAutomatically = true)
  @Query("update User u set u.groupId = :groupId where u.id in :userIds and u.groupId is null")
  int assignToGroupIfUnassigned(@Param("groupId") Long groupId, @Param("userIds") List<Long> userIds);
}
