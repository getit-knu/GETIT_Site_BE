package com.getit.domain.user.service;

import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.UserStatus;
import com.getit.domain.user.repository.UserRepository;
import java.util.List;
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
}
