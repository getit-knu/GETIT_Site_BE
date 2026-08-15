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
}
