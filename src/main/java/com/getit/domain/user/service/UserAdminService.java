package com.getit.domain.user.service;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.GroupSummary;
import com.getit.domain.user.dto.UserExportFilter;
import com.getit.domain.user.dto.UserSummary;
import com.getit.domain.user.dto.UserUpdateCommand;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.domain.user.util.ExcelExporter;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 사용자 목록 조회 · 권한 관리 · 삭제. (API 명세서 9.1 · 9.2 · 9.3) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminService {

  /** 9.1 groupId 쿼리 파라미터에서 미배정자만 조회할 때 쓰는 값. */
  private static final String UNASSIGNED_GROUP_FILTER = "none";

  private static final List<String> EXCEL_HEADERS =
      List.of("이름", "이메일", "연락처", "단과대학", "전공", "학년", "권한", "기수", "그룹", "가입일", "상태");
  private static final DateTimeFormatter EXCEL_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final UserRepository userRepository;
  private final GroupRepository groupRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * 9.1. {@code groupId} 는 세 가지 상태를 표현한다 — 미전달(필터 없음) · {@code "none"}(미배정만) ·
   * 숫자(특정 조). 숫자가 아니면 {@code INVALID_GROUP_FILTER} 를 던진다.
   *
   * <p>{@code pageable} 에 {@code id} 정렬을 tie-breaker 로 추가한다 — 클라이언트가 요청한
   * 정렬 기준(예: 이름순)만으로는 같은 값을 가진 행이 여럿일 때 순서가 실행 계획에 따라
   * 달라질 수 있어, 페이지 사이에 사용자가 추가되면 같은 사용자가 중복되거나 누락될 수 있다
   * (PR #62 Copilot 리뷰 지적 — {@code ApplicationAdminService} 와 동일한 이유). 다만 7.1과
   * 달리 여기는 클라이언트의 정렬 기준 자체를 강제로 덮어쓰지 않는다 — 9.1은 7.5(순차탐색)처럼
   * "목록과 같은 순서여야 하는" 다른 API와 엮여있지 않기 때문이다.
   */
  public PageResponse<UserSummary> listUsers(
      String keyword, Role role, String groupId, Integer generationNo, Pageable pageable
  ) {
    boolean unassignedOnly = UNASSIGNED_GROUP_FILTER.equalsIgnoreCase(groupId);
    Long targetGroupId = parseGroupId(groupId, unassignedOnly);

    Page<User> users = userRepository.searchUsers(
        blankToNull(keyword), role, generationNo, targetGroupId, unassignedOnly, withIdTiebreaker(pageable));

    Map<Long, GroupSummary> groupsById = loadGroups(users.getContent());

    return PageResponse.from(users, user -> UserSummary.from(user, groupsById.get(user.getGroupId())));
  }

  private Pageable withIdTiebreaker(Pageable pageable) {
    boolean hasIdOrder = pageable.getSort().stream().anyMatch(order -> order.getProperty().equals("id"));
    Sort sort = hasIdOrder ? pageable.getSort() : pageable.getSort().and(Sort.by(Sort.Direction.ASC, "id"));
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
  }

  /**
   * 9.5. 9.1 과 동일한 필터로 페이징 없이 전체를 한 시트에 담는다 (7.6 과 같은 이유).
   *
   * <p>필터를 {@link UserExportFilter} 로 묶어서 받는다 — {@code keyword} 와 {@code groupId} 가
   * 둘 다 {@code String} 이라, 네 인자를 그대로 받으면 호출부에서 순서가 바뀌어도 컴파일 에러 없이
   * 통과한다(PR #71 Copilot 리뷰 지적).
   */
  public byte[] exportUsersExcel(UserExportFilter filter) {
    boolean unassignedOnly = UNASSIGNED_GROUP_FILTER.equalsIgnoreCase(filter.groupId());
    Long targetGroupId = parseGroupId(filter.groupId(), unassignedOnly);

    Page<User> users = userRepository.searchUsers(
        blankToNull(filter.keyword()), filter.role(), filter.generationNo(), targetGroupId, unassignedOnly,
        Pageable.unpaged(Sort.by(Sort.Direction.ASC, "id")));

    Map<Long, GroupSummary> groupsById = loadGroups(users.getContent());

    List<List<Object>> rows = users.getContent().stream()
        .map(user -> toExcelRow(user, groupsById.get(user.getGroupId())))
        .toList();

    return ExcelExporter.toXlsx("사용자 목록", EXCEL_HEADERS, rows);
  }

  private List<Object> toExcelRow(User user, GroupSummary group) {
    return List.<Object>of(
        user.getName(),
        user.getEmail(),
        user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
        user.getCollege() != null ? user.getCollege() : "",
        user.getMajor() != null ? user.getMajor() : "",
        user.getStudentYear() != null ? user.getStudentYear() : "",
        user.getRole().getLabel(),
        user.getGenerationNo() != null ? user.getGenerationNo() : "",
        group != null ? group.name() : "",
        user.getCreatedAt() != null ? user.getCreatedAt().format(EXCEL_DATE_FORMAT) : "",
        user.getStatus().getLabel()
    );
  }

  /**
   * 9.2. role · groupId · generationNo 중 null 이 아닌 필드만 변경한다(partial update).
   *
   * <p>운영진 본인이 자신의 ADMIN 권한을 해제하는 요청은 거부한다 (최소 1명의 ADMIN 을
   * 보장하기 위함, 명세서 9.2).
   *
   * <p>{@code unassignGroup} 은 조 배정을 푼다. {@code groupId} 의 {@code null} 이 이미
   * "안 건드림" 으로 쓰이고 있어 해제를 표현할 자리가 없었다 — 어드민 화면의 "미배정" 을
   * 골라도 아무 일이 일어나지 않았다 (이슈 #174).
   *
   * <p>조원 빼기(9.11 {@code DELETE /admin/groups/{groupId}/members/{userId}})로도 같은 일을
   * 할 수 있다. 그쪽은 조 관리 화면에서 명단을 보며 빼는 자리이고, 이쪽은 사용자 한 줄에서
   * 바꾸는 자리다. 둘 다 {@code User.leaveGroup()} 하나로 모인다.
   */
  @Transactional
  public UserSummary updateUser(Long targetUserId, Long currentUserId, UserUpdateCommand command) {
    Role role = command.role();
    Long groupId = command.groupId();
    Integer generationNo = command.generationNo();
    boolean unassignGroup = command.unassignGroup();

    User user = findUser(targetUserId);

    if (unassignGroup && groupId != null) {
      throw new BusinessException(UserErrorCode.GROUP_ASSIGN_CONFLICT);
    }
    if (role != null) {
      validateNotSelfAdminRevocation(user, targetUserId, currentUserId, role);
    }
    if (groupId != null || generationNo != null) {
      validateGroupGenerationConsistency(user, groupId, generationNo, unassignGroup);
    }

    if (role != null) {
      user.updateRole(role);
    }
    if (unassignGroup) {
      user.leaveGroup();
    }
    if (groupId != null) {
      user.assignToGroup(groupId);
    }
    if (generationNo != null) {
      user.updateGenerationNo(generationNo);
    }

    GroupSummary group = user.getGroupId() != null
        ? groupRepository.findById(user.getGroupId()).map(GroupSummary::from).orElse(null)
        : null;

    return UserSummary.from(user, group);
  }

  /**
   * 변경 후 사용자가 속하게 될 조와 기수가 서로 다른 기수를 가리키면 안 된다. 검증 없이
   * 허용하면, 9.6 조회(조의 기수와 사용자의 generationNo 가 같아야 조원 명단에 나타남)에서
   * 이 사용자가 조원 · 미배정자 어느 쪽에도 안 나타나는 유령 데이터가 된다. 존재하지 않는
   * generationNo 도 여기서 함께 막는다 (PR #62 Copilot 리뷰 지적).
   */
  private void validateGroupGenerationConsistency(
      User user, Long groupId, Integer generationNo, boolean unassignGroup
  ) {
    Integer effectiveGenerationNo = generationNo != null ? generationNo : user.getGenerationNo();
    // 해제하면 조가 없어지므로 조-기수 일치를 따질 대상도 없다.
    Long effectiveGroupId = unassignGroup ? null : (groupId != null ? groupId : user.getGroupId());

    if (generationNo != null) {
      findGenerationByNo(generationNo);
    }
    if (effectiveGroupId != null) {
      Group group = findGroup(effectiveGroupId);
      GenerationSummary groupGeneration = findGeneration(group.getGenerationId());
      if (!groupGeneration.generationNo().equals(effectiveGenerationNo)) {
        throw new BusinessException(UserErrorCode.GROUP_GENERATION_MISMATCH);
      }
    }
  }

  /**
   * 9.3. 지원서 · 과제 제출 · Q&A 이력 보존을 위해 soft delete 한다.
   *
   * <p>9.2 는 자기 자신의 ADMIN 권한 해제를 막지만, 삭제 API에 이 보호가 없으면 같은 결과를
   * "권한 변경" 대신 "삭제"로 우회할 수 있다 (PR #62 Copilot 리뷰 지적). 대상이 ADMIN 인지
   * 여부와 무관하게 자기 자신은 이 엔드포인트로 탈퇴시킬 수 없게 막는다 — 9.2 의 자기 강등
   * 방지가 "다른 ADMIN 이 남아있는지"를 따지지 않는 것과 동일하게, 단순하고 예측 가능한
   * 규칙을 유지하기 위함이다.
   */
  @Transactional
  public void deleteUser(Long userId, Long currentUserId) {
    User user = findUser(userId);
    if (userId.equals(currentUserId)) {
      throw new BusinessException(UserErrorCode.CANNOT_REMOVE_OWN_ADMIN, "자기 자신은 삭제할 수 없습니다.");
    }
    user.withdraw();
  }

  private void validateNotSelfAdminRevocation(User user, Long targetUserId, Long currentUserId, Role newRole) {
    boolean isSelf = targetUserId.equals(currentUserId);
    boolean losesAdmin = user.getRole() == Role.ADMIN && newRole != Role.ADMIN;
    if (isSelf && losesAdmin) {
      throw new BusinessException(UserErrorCode.CANNOT_REMOVE_OWN_ADMIN);
    }
  }

  private Long parseGroupId(String groupId, boolean unassignedOnly) {
    if (groupId == null || unassignedOnly) {
      return null;
    }
    try {
      return Long.valueOf(groupId);
    } catch (NumberFormatException e) {
      throw new BusinessException(UserErrorCode.INVALID_GROUP_FILTER);
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

  private GenerationSummary findGeneration(Long generationId) {
    return generationQueryService.findById(generationId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GENERATION_NOT_FOUND));
  }

  private GenerationSummary findGenerationByNo(Integer generationNo) {
    return generationQueryService.findByGenerationNo(generationNo)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GENERATION_NOT_FOUND));
  }
}
