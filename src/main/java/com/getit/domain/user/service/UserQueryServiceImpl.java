package com.getit.domain.user.service;

import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.domain.user.repository.UserRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

  private final UserRepository userRepository;

  @Override
  public List<MemberSummary> findActiveMembers(Integer generationNo) {
    return userRepository.findByRoleAndStatusAndGenerationNo(Role.MEMBER, UserStatus.ACTIVE, generationNo)
        .stream()
        .map(MemberSummary::from)
        .toList();
  }

  @Override
  public long countActiveMembers() {
    return userRepository.countByRoleAndStatus(Role.MEMBER, UserStatus.ACTIVE);
  }

  @Override
  public long countActiveMembersInGeneration(Integer generationNo) {
    return userRepository.countByRoleAndStatusAndGenerationNo(Role.MEMBER, UserStatus.ACTIVE, generationNo);
  }

  @Override
  public Map<Long, String> findNamesByIds(Collection<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userRepository.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, User::getName));
  }
}
