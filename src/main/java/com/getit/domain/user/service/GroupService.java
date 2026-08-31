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
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 조 관리. (API 명세서 9.6 ~ 9.11) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService implements MemberGroupService {

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * 부원이 보는 내 조. (이슈 #148 — GET /api/member/group)
   *
   * <p>조 배정은 어드민이 한다. 아직 배정되지 않은 부원이 있는 것은 정상이라 오류로 다루지 않는다.
   */
  @Override
  public Optional<GroupWithMembersResult> findMyGroup(Long userId) {
    User user = userRepository.findById(userId)
        .filter(found -> !found.isDeleted())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    if (user.getGroupId() == null) {
      return Optional.empty();
    }

    Group group = groupRepository.findById(user.getGroupId())
        .orElseThrow(() -> new BusinessException(UserErrorCode.GROUP_NOT_FOUND));
    List<GroupMemberResult> members =
        userRepository.findByGroupIdAndStatus(group.getId(), UserStatus.ACTIVE).stream()
            .map(GroupMemberResult::from)
            .toList();

    return Optional.of(GroupWithMembersResult.of(group, members));
  }

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

  /**
   * 9.7. {@code validateNameNotDuplicate} 선검사와 실제 저장 사이에도 다른 요청이 같은 이름을
   * 먼저 저장할 수 있다. DB 유니크 제약이 최종 방어선이지만, 그 위반을 잡지 않으면 약속한
   * {@code DUPLICATE_GROUP_NAME} 409 대신 500이 된다 (PR #60 Copilot 리뷰 지적 — suppressed
   * comment). {@code saveAndFlush} 로 이 메서드 안에서 즉시 제약을 확인해 잡아낸다.
   */
  @Transactional
  public GroupResult createGroup(Long generationId, String name) {
    GenerationSummary generation = findGeneration(generationId);
    validateNameNotDuplicate(generation.id(), name, null);

    Group saved;
    try {
      saved = groupRepository.saveAndFlush(Group.create(generation.id(), name));
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.DUPLICATE_GROUP_NAME);
    }

    return GroupResult.of(saved, generation.generationNo(), 0);
  }

  /**
   * 9.8. 소속 기수는 바꾸지 않는다. {@code createGroup} 과 같은 이유로 즉시 flush 해서
   * 이름 중복 경합을 500이 아니라 {@code DUPLICATE_GROUP_NAME} 409로 잡는다.
   */
  @Transactional
  public GroupResult renameGroup(Long groupId, String name) {
    Group group = findGroup(groupId);
    validateNameNotDuplicate(group.getGenerationId(), name, groupId);
    group.rename(name);

    try {
      groupRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.DUPLICATE_GROUP_NAME);
    }

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

  /**
   * 9.10. 대상 중 이미 이 조에 속한 사용자는 멱등하게 건너뛰고("다른 조"가 아니므로 충돌이
   * 아니다), 이미 다른 조에 속한 사용자가 있으면 전체를 거부한다.
   *
   * <p>배정은 "미배정인지 확인 후 배정"을 자바에서 두 단계로 하지 않고
   * {@link UserRepository#assignToGroupIfUnassigned} 원자적 UPDATE 하나로 한다 — 두 단계로
   * 하면 확인과 반영 사이에 다른 요청이 같은 사용자를 먼저 배정해버리는 경합이 생긴다
   * (PR #60 Copilot 리뷰 지적). 반영된 행 수가 기대치보다 적으면 그 경합이 실제로 일어난
   * 것이므로 예외를 던져 트랜잭션 전체를 롤백한다(all-or-nothing 계약 유지).
   */
  @Transactional
  public GroupMemberAddResult addMembers(Long groupId, List<Long> userIds) {
    Group group = findGroup(groupId);
    List<User> users = userRepository.findAllById(userIds);
    if (users.size() != userIds.size()) {
      throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }
    validateEligibleForGroup(group, users);

    List<Long> toAssign = users.stream()
        .filter(user -> !groupId.equals(user.getGroupId()))
        .map(User::getId)
        .toList();

    int existingCount = countActiveMembers(groupId);

    if (!toAssign.isEmpty()) {
      int updated = userRepository.assignToGroupIfUnassigned(groupId, toAssign);
      if (updated != toAssign.size()) {
        throw new BusinessException(UserErrorCode.ALREADY_IN_GROUP);
      }
    }

    return new GroupMemberAddResult(groupId, toAssign.size(), existingCount + toAssign.size());
  }

  /**
   * 9.11. {@code userId} 가 실제로 이 {@code groupId} 소속인지 확인한다 — 확인 없이 빼면 다른
   * 조 소속 사용자의 id를 넣어도 그 배정이 해제돼버린다 (PR #60 Copilot 리뷰 지적).
   */
  @Transactional
  public void removeMember(Long groupId, Long userId) {
    findGroup(groupId);
    User user = userRepository.findById(userId)
        .filter(u -> groupId.equals(u.getGroupId()))
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.leaveGroup();
  }

  /**
   * 조의 소속 기수와 다르거나 활성 상태가 아닌 사용자는 조에 배정할 수 없다. 검증 없이 배정하면
   * 9.6 보드 조회(대상 기수의 ACTIVE 사용자만 읽음)에 이 사용자가 어느 명단에도 나타나지 않고
   * memberCount 도 실제와 달라진다 (PR #60 Copilot 리뷰 지적). GUEST 는 9.6 응답 예시에도 조원으로
   * 나오므로 role 은 제한하지 않는다.
   */
  private void validateEligibleForGroup(Group group, List<User> users) {
    GenerationSummary generation = findGeneration(group.getGenerationId());
    boolean allEligible = users.stream().allMatch(user ->
        generation.generationNo().equals(user.getGenerationNo()) && user.getStatus() == UserStatus.ACTIVE);
    if (!allEligible) {
      throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
    }
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

  /**
   * 9.8~9.11(이름 변경 · 삭제 · 조원 변경)이 조를 찾을 때 쓴다. 활성 기수로 스코프한다 —
   * id만으로 찾으면 과거 기수의 조 id를 알기만 해도 이름 변경 · 삭제 · 조원 변경이 가능해진다
   * (PR #60 Copilot 리뷰 지적, 처음엔 9.7이 임의 기수 조 생성을 허용하는 것과 비일관적이라고
   * 판단해 보류했으나, 팀 논의 후 반영하기로 결정했다 — 생성(9.7)은 그대로 임의 기수를
   * 허용하고, 생성된 조에 대한 수정만 활성 기수로 제한한다). 다른 기수의 조는 존재해도
   * GROUP_NOT_FOUND 로 처리한다.
   */
  private Group findGroup(Long groupId) {
    GenerationSummary active = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(UserErrorCode.ACTIVE_GENERATION_NOT_FOUND));
    return groupRepository.findByIdAndGenerationId(groupId, active.id())
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
