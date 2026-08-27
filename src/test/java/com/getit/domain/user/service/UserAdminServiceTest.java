package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class UserAdminServiceTest {

  @Autowired
  private UserAdminService userAdminService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private GenerationRepository generationRepository;

  /** 9.2 의 groupId·generationNo 정합성 검증이 실제 Generation 행을 요구하므로 미리 만들어둔다. */
  private Generation generation9;

  @BeforeEach
  void setUpGeneration() {
    generation9 = generationRepository.save(Generation.create(9, 2026));
  }

  private User guest(String providerId, String email, String name) {
    return userRepository.save(User.createGuest(providerId, email, name, "https://cdn.getit.com/1.png"));
  }

  @Nested
  @DisplayName("listUsers")
  class ListUsers {

    @Test
    @DisplayName("keyword · role · generationNo 로 필터링한다")
    void filtersByKeywordRoleGenerationNo() {
      User target = guest("google-1", "a@getit.com", "김부원");
      target.promoteToMember(9);
      guest("google-2", "b@getit.com", "이회원");

      PageResponse<UserSummary> result = userAdminService.listUsers(
          "김", Role.MEMBER, null, 9, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(UserSummary::name).containsExactly("김부원");
    }

    @Test
    @DisplayName("groupId 가 특정 값이면 그 조 소속만 반환하고 group 필드를 채운다")
    void filtersBySpecificGroupId() {
      Group group = groupRepository.save(Group.create(1L, "1조"));
      User grouped = guest("google-3", "c@getit.com", "조원");
      grouped.assignToGroup(group.getId());
      guest("google-4", "d@getit.com", "미배정");

      PageResponse<UserSummary> result = userAdminService.listUsers(
          null, null, String.valueOf(group.getId()), null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(UserSummary::name).containsExactly("조원");
      assertThat(result.content().get(0).group().name()).isEqualTo("1조");
    }

    @Test
    @DisplayName("groupId 가 none 이면 미배정 사용자만 반환한다")
    void filtersUnassignedOnly() {
      Group group = groupRepository.save(Group.create(1L, "1조"));
      User grouped = guest("google-5", "e@getit.com", "조원");
      grouped.assignToGroup(group.getId());
      guest("google-6", "f@getit.com", "미배정");

      PageResponse<UserSummary> result = userAdminService.listUsers(
          null, null, "none", null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(UserSummary::name).containsExactly("미배정");
      assertThat(result.content().get(0).group()).isNull();
    }

    @Test
    @DisplayName("groupId 가 숫자도 none 도 아니면 예외가 발생한다")
    void throwsWhenGroupIdInvalid() {
      assertThatThrownBy(() -> userAdminService.listUsers(
          null, null, "abc", null, PageRequest.of(0, 20)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.INVALID_REQUEST);
    }
  }

  @Nested
  @DisplayName("updateUser")
  class UpdateUser {

    @Test
    @DisplayName("role 만 보내면 role 만 바뀐다")
    void updatesRoleOnly() {
      User user = guest("google-7", "g@getit.com", "부원");

      UserSummary result = userAdminService.updateUser(user.getId(), 999L, Role.MEMBER, null, null);

      assertThat(result.role()).isEqualTo(Role.MEMBER);
      assertThat(user.getGroupId()).isNull();
    }

    @Test
    @DisplayName("groupId 를 보내면 조가 배정되고 응답에 group 이 채워진다")
    void updatesGroup() {
      Group group = groupRepository.save(Group.create(generation9.getId(), "1조"));
      User user = guest("google-8", "h@getit.com", "부원");
      user.promoteToMember(9);

      UserSummary result = userAdminService.updateUser(user.getId(), 999L, null, group.getId(), null);

      assertThat(result.group().id()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("없는 groupId 면 예외가 발생한다")
    void throwsWhenGroupNotFound() {
      User user = guest("google-9", "i@getit.com", "부원");

      assertThatThrownBy(() -> userAdminService.updateUser(user.getId(), 999L, null, 999L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("generationNo 만 보내면 generationNo 만 바뀐다")
    void updatesGenerationNoOnly() {
      User user = guest("google-10", "j@getit.com", "부원");

      UserSummary result = userAdminService.updateUser(user.getId(), 999L, null, null, 9);

      assertThat(result.generationNo()).isEqualTo(9);
    }

    @Test
    @DisplayName("존재하지 않는 generationNo 면 예외가 발생한다")
    void throwsWhenGenerationNotFound() {
      User user = guest("google-16", "q@getit.com", "부원");

      assertThatThrownBy(() -> userAdminService.updateUser(user.getId(), 999L, null, null, 999))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("조의 소속 기수와 사용자의 기수가 다르면 예외가 발생한다")
    void throwsWhenGroupGenerationMismatch() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Group group = groupRepository.save(Group.create(otherGeneration.getId(), "1조"));
      User user = guest("google-17", "r@getit.com", "부원");
      user.promoteToMember(9);

      assertThatThrownBy(() -> userAdminService.updateUser(user.getId(), 999L, null, group.getId(), null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_GENERATION_MISMATCH);
    }

    @Test
    @DisplayName("이미 조에 속한 사용자의 generationNo 만 바꾸면 조 기수와 어긋나 예외가 발생한다")
    void throwsWhenChangingGenerationNoBreaksExistingGroupMembership() {
      Group group = groupRepository.save(Group.create(generation9.getId(), "1조"));
      User user = guest("google-18", "s@getit.com", "부원");
      user.promoteToMember(9);
      user.assignToGroup(group.getId());
      generationRepository.save(Generation.create(8, 2025));

      assertThatThrownBy(() -> userAdminService.updateUser(user.getId(), 999L, null, null, 8))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_GENERATION_MISMATCH);

      // 실패한 요청은 반영되지 않아야 한다.
      assertThat(user.getGenerationNo()).isEqualTo(9);
    }

    @Test
    @DisplayName("없는 사용자면 예외가 발생한다")
    void throwsWhenUserNotFound() {
      assertThatThrownBy(() -> userAdminService.updateUser(999L, 999L, Role.MEMBER, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("본인의 ADMIN 권한을 스스로 해제하면 예외가 발생한다")
    void rejectsSelfAdminRevocation() {
      User admin = guest("google-11", "k@getit.com", "운영진");
      admin.updateRole(Role.ADMIN);

      assertThatThrownBy(() -> userAdminService.updateUser(
          admin.getId(), admin.getId(), Role.MEMBER, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.CANNOT_REMOVE_OWN_ADMIN);

      assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    @DisplayName("다른 관리자가 ADMIN 권한을 해제하는 것은 허용된다")
    void allowsOtherAdminToRevokeAdmin() {
      User admin = guest("google-12", "l@getit.com", "운영진");
      admin.updateRole(Role.ADMIN);
      User otherAdmin = guest("google-13", "m@getit.com", "다른 운영진");
      otherAdmin.updateRole(Role.ADMIN);

      UserSummary result = userAdminService.updateUser(
          admin.getId(), otherAdmin.getId(), Role.MEMBER, null, null);

      assertThat(result.role()).isEqualTo(Role.MEMBER);
    }

    @Test
    @DisplayName("본인이어도 ADMIN 을 ADMIN 으로 다시 보내면 허용된다")
    void allowsSelfUpdateWhenStillAdmin() {
      User admin = guest("google-14", "n@getit.com", "운영진");
      admin.updateRole(Role.ADMIN);

      UserSummary result = userAdminService.updateUser(
          admin.getId(), admin.getId(), Role.ADMIN, null, 9);

      assertThat(result.role()).isEqualTo(Role.ADMIN);
      assertThat(result.generationNo()).isEqualTo(9);
    }
  }

  @Nested
  @DisplayName("deleteUser")
  class DeleteUser {

    @Test
    @DisplayName("사용자를 soft delete 한다")
    void softDeletesUser() {
      User user = guest("google-15", "o@getit.com", "부원");

      userAdminService.deleteUser(user.getId());

      User found = userRepository.findById(user.getId()).orElseThrow();
      assertThat(found.isDeleted()).isTrue();
      assertThat(found.getStatus().name()).isEqualTo("WITHDRAWN");
    }

    @Test
    @DisplayName("없는 사용자면 예외가 발생한다")
    void throwsWhenUserNotFound() {
      assertThatThrownBy(() -> userAdminService.deleteUser(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }
  }
}
