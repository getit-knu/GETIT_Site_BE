package com.getit.domain.user.service;

import com.getit.domain.user.dto.GroupSummary;
import com.getit.domain.user.dto.UserSummary;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 사용자 목록 조회 · 권한 관리 · 삭제. (API 명세서 9.1 · 9.2 · 9.3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminService {

  /** 9.1 groupId 쿼리 파라미터에서 미배정자만 조회할 때 쓰는 값. */
  private static final String UNASSIGNED_GROUP_FILTER = "none";

  private final UserRepository userRepository;
  private final GroupRepository groupRepository;

  /**
   * 9.1. {@code groupId} 는 세 가지 상태를 표현한다 — 미전달(필터 없음) · {@code "none"}(미배정만) ·
   * 숫자(특정 조). 숫자가 아니면 {@code INVALID_REQUEST} 를 던진다.
   */
  public PageResponse<UserSummary> listUsers(
      String keyword, Role role, String groupId, Integer generationNo, Pageable pageable
  ) {
    boolean unassignedOnly = UNASSIGNED_GROUP_FILTER.equalsIgnoreCase(groupId);
    Long targetGroupId = parseGroupId(groupId, unassignedOnly);

    Page<User> users = userRepository.searchUsers(
        blankToNull(keyword), role, generationNo, targetGroupId, unassignedOnly, pageable);

    Map<Long, GroupSummary> groupsById = loadGroups(users.getContent());

    return PageResponse.from(users, user -> UserSummary.from(user, groupsById.get(user.getGroupId())));
  }

  /**
   * 9.2. role · groupId · generationNo 중 null 이 아닌 필드만 변경한다(partial update).
   *
   * <p>운영진 본인이 자신의 ADMIN 권한을 해제하는 요청은 거부한다 (최소 1명의 ADMIN 을
   * 보장하기 위함, 명세서 9.2).
   */
  @Transactional
  public UserSummary updateUser(
      Long targetUserId, Long currentUserId, Role role, Long groupId, Integer generationNo
  ) {
    User user = findUser(targetUserId);

    if (role != null) {
      validateNotSelfAdminRevocation(user, targetUserId, currentUserId, role);
      user.updateRole(role);
    }
    if (groupId != null) {
      Group group = findGroup(groupId);
      user.assignToGroup(group.getId());
    }
    if (generationNo != null) {
      user.updateGenerationNo(generationNo);
    }

    GroupSummary group = user.getGroupId() != null
        ? groupRepository.findById(user.getGroupId()).map(GroupSummary::from).orElse(null)
        : null;

    return UserSummary.from(user, group);
  }

  /** 9.3. 지원서 · 과제 제출 · Q&A 이력 보존을 위해 soft delete 한다. */
  @Transactional
  public void deleteUser(Long userId) {
    findUser(userId).withdraw();
  }

  private void validateNotSelfAdminRevocation(User user, Long targetUserId, Long currentUserId, Role newRole) {
    boolean isSelf = targetUserId.equals(currentUserId);
    boolean losesAdmin = user.getRole() == Role.ADMIN && newRole != Role.ADMIN;
    if (isSelf && losesAdmin) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN, "자기 자신의 ADMIN 권한은 해제할 수 없습니다.");
    }
  }

  private Long parseGroupId(String groupId, boolean unassignedOnly) {
    if (groupId == null || unassignedOnly) {
      return null;
    }
    try {
      return Long.valueOf(groupId);
    } catch (NumberFormatException e) {
      throw new BusinessException(CommonErrorCode.INVALID_REQUEST, "groupId 는 숫자 또는 'none' 이어야 합니다.");
    }
  }

  /**
   * {@code Map.of()} (빈 불변 맵)는 null 키 조회에서 NPE 를 던지므로, 사용자의 groupId 가
   * null 일 수 있는 이 조회에는 쓸 수 없다 — {@link Collections#emptyMap()} 을 쓴다.
   */
  private Map<Long, GroupSummary> loadGroups(List<User> users) {
    List<Long> groupIds = users.stream().map(User::getGroupId).filter(Objects::nonNull).distinct().toList();
    if (groupIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return groupRepository.findAllById(groupIds).stream()
        .collect(Collectors.toMap(Group::getId, GroupSummary::from));
  }

  private String blankToNull(String keyword) {
    return (keyword == null || keyword.isBlank()) ? null : keyword;
  }

  private User findUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }

  private Group findGroup(Long groupId) {
    return groupRepository.findById(groupId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GROUP_NOT_FOUND));
  }
}
