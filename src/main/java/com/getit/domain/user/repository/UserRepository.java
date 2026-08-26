package com.getit.domain.user.repository;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
