package com.getit.domain.user.service;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.GroupBoardResult;
import com.getit.domain.user.dto.GroupMemberAddResult;
import com.getit.domain.user.dto.GroupMemberResult;
import com.getit.domain.user.dto.GroupResult;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 조 관리. (API 명세서 9.6 ~ 9.11) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final GenerationQueryService generationQueryService;

  /** 9.6. generationId 가 없으면 활성 기수를 쓴다. */
  public GroupBoardResult getGroups(Long generationId) {
    GenerationSummary generation = resolveGeneration(generationId);

    List<Group> groups = groupRepository.findByGenerationIdOrderByIdAsc(generation.id());
    List<User> activeUsers = userRepository.findByGenerationNoAndStatus(
        generation.generationNo(), UserStatus.ACTIVE);

    Map<Long, List<GroupMemberResult>> membersByGroupId = activeUsers.stream()
        .filter(user -> user.getGroupId() != null)
        .collect(Collectors.groupingBy(
            User::getGroupId,
            Collectors.mapping(GroupMemberResult::from, Collectors.toList())));

    List<GroupWithMembersResult> groupResults = groups.stream()
        .map(group -> GroupWithMembersResult.of(
            group, membersByGroupId.getOrDefault(group.getId(), List.of())))
        .toList();

    List<GroupMemberResult> unassigned = activeUsers.stream()
        .filter(user -> user.getGroupId() == null)
        .map(GroupMemberResult::from)
        .toList();

    return new GroupBoardResult(generation.generationNo(), groupResults, unassigned);
  }

  /** 9.7. */
  @Transactional
  public GroupResult createGroup(Long generationId, String name) {
    GenerationSummary generation = findGeneration(generationId);
    validateNameNotDuplicate(generation.id(), name, null);

    Group saved = groupRepository.save(Group.create(generation.id(), name));

    return GroupResult.of(saved, generation.generationNo(), 0);
  }

  /** 9.8. 소속 기수는 바꾸지 않는다. */
  @Transactional
  public GroupResult renameGroup(Long groupId, String name) {
    Group group = findGroup(groupId);
    validateNameNotDuplicate(group.getGenerationId(), name, groupId);
    group.rename(name);

    GenerationSummary generation = findGeneration(group.getGenerationId());

    return GroupResult.of(group, generation.generationNo(), countActiveMembers(groupId));
  }

  /** 9.9. 소속 인원은 조를 지워도 삭제되지 않고 미배정 상태가 된다. */
  @Transactional
  public void deleteGroup(Long groupId) {
    Group group = findGroup(groupId);

    userRepository.findByGroupId(groupId).forEach(User::leaveGroup);
    groupRepository.delete(group);
  }

  /** 9.10. 대상 중 하나라도 이미 다른 조에 속해 있으면 전체를 거부한다. */
  @Transactional
  public GroupMemberAddResult addMembers(Long groupId, List<Long> userIds) {
    findGroup(groupId);
    List<User> users = userRepository.findAllById(userIds);
    if (users.size() != userIds.size()) {
      throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }
    if (users.stream().anyMatch(user -> user.getGroupId() != null)) {
      throw new BusinessException(UserErrorCode.ALREADY_IN_GROUP);
    }

    int existingCount = countActiveMembers(groupId);
    users.forEach(user -> user.assignToGroup(groupId));

    return new GroupMemberAddResult(groupId, users.size(), existingCount + users.size());
  }

  /** 9.11. */
  @Transactional
  public void removeMember(Long groupId, Long userId) {
    findGroup(groupId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.leaveGroup();
  }

  private void validateNameNotDuplicate(Long generationId, String name, Long excludeGroupId) {
    boolean duplicate = excludeGroupId == null
        ? groupRepository.existsByGenerationIdAndName(generationId, name)
        : groupRepository.existsByGenerationIdAndNameAndIdNot(generationId, name, excludeGroupId);
    if (duplicate) {
      throw new BusinessException(UserErrorCode.DUPLICATE_GROUP_NAME);
    }
  }

  private int countActiveMembers(Long groupId) {
    return userRepository.findByGroupIdAndStatus(groupId, UserStatus.ACTIVE).size();
  }

  private Group findGroup(Long groupId) {
    return groupRepository.findById(groupId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GROUP_NOT_FOUND));
  }

  private GenerationSummary findGeneration(Long generationId) {
    return generationQueryService.findById(generationId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GENERATION_NOT_FOUND));
  }

  private GenerationSummary resolveGeneration(Long generationId) {
    if (generationId != null) {
      return findGeneration(generationId);
    }
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(UserErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
