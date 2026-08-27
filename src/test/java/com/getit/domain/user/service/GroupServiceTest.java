package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.dto.GroupBoardResult;
import com.getit.domain.user.dto.GroupMemberAddResult;
import com.getit.domain.user.dto.GroupResult;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class GroupServiceTest {

  @Autowired
  private GroupService groupService;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private User member(String providerId, String email, String major) {
    User user = userRepository.save(
        User.createGuest(providerId, email, "부원", "https://cdn.getit.com/1.png"));
    user.promoteToMember(activeGeneration.getGenerationNo());
    user.updateApplicantInfo(null, null, major, null, null);
    return user;
  }

  @Nested
  @DisplayName("getGroups")
  class GetGroups {

    @Test
    @DisplayName("generationId 없이 조회하면 활성 기수를 쓴다")
    void usesActiveGenerationWhenNoIdGiven() {
      groupRepository.save(Group.create(activeGeneration.getId(), "1조"));

      GroupBoardResult result = groupService.getGroups(null);

      assertThat(result.generationNo()).isEqualTo(9);
      assertThat(result.groups()).extracting(g -> g.name()).containsExactly("1조");
    }

    @Test
    @DisplayName("조원은 조별로, 미배정자는 unassigned 로 나뉜다")
    void splitsMembersByGroup() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User grouped = member("google-1", "a@getit.com", "경영학과");
      grouped.assignToGroup(group.getId());
      member("google-2", "b@getit.com", "컴퓨터공학과");

      GroupBoardResult result = groupService.getGroups(activeGeneration.getId());

      assertThat(result.groups()).hasSize(1);
      assertThat(result.groups().get(0).memberCount()).isEqualTo(1);
      assertThat(result.groups().get(0).members()).extracting(m -> m.userId())
          .containsExactly(grouped.getId());
      assertThat(result.unassigned()).extracting(m -> m.name()).containsExactly("부원");
    }

    @Test
    @DisplayName("활성 기수가 없고 generationId 도 없으면 예외가 발생한다")
    void throwsWhenNoActiveGenerationAndNoId() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> groupService.getGroups(null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 generationId 를 명시하면 예외가 발생한다")
    void throwsWhenGenerationNotFound() {
      assertThatThrownBy(() -> groupService.getGroups(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createGroup")
  class CreateGroup {

    @Test
    @DisplayName("조원 0명인 조가 생성된다")
    void createsGroupWithNoMembers() {
      GroupResult result = groupService.createGroup(activeGeneration.getId(), "1조");

      assertThat(result.name()).isEqualTo("1조");
      assertThat(result.generationNo()).isEqualTo(9);
      assertThat(result.memberCount()).isZero();
    }

    @Test
    @DisplayName("같은 기수 안에 이름이 중복되면 예외가 발생한다")
    void rejectsDuplicateNameWithinGeneration() {
      groupService.createGroup(activeGeneration.getId(), "1조");

      assertThatThrownBy(() -> groupService.createGroup(activeGeneration.getId(), "1조"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.DUPLICATE_GROUP_NAME);
    }

    @Test
    @DisplayName("다른 기수라면 이름이 같아도 허용된다")
    void allowsSameNameInDifferentGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      groupService.createGroup(activeGeneration.getId(), "1조");

      GroupResult result = groupService.createGroup(otherGeneration.getId(), "1조");

      assertThat(result.name()).isEqualTo("1조");
    }
  }

  @Nested
  @DisplayName("renameGroup")
  class RenameGroup {

    @Test
    @DisplayName("이름을 수정하고 소속 기수는 유지한다")
    void renamesKeepsGeneration() {
      GroupResult created = groupService.createGroup(activeGeneration.getId(), "1조");

      GroupResult renamed = groupService.renameGroup(created.id(), "A조");

      assertThat(renamed.name()).isEqualTo("A조");
      assertThat(renamed.generationNo()).isEqualTo(9);
    }

    @Test
    @DisplayName("없는 조를 수정하면 예외가 발생한다")
    void throwsWhenGroupNotFound() {
      assertThatThrownBy(() -> groupService.renameGroup(999L, "A조"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 조와 이름이 겹치면 예외가 발생한다")
    void rejectsDuplicateNameFromOtherGroup() {
      groupService.createGroup(activeGeneration.getId(), "1조");
      GroupResult second = groupService.createGroup(activeGeneration.getId(), "2조");

      assertThatThrownBy(() -> groupService.renameGroup(second.id(), "1조"))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.DUPLICATE_GROUP_NAME);
    }
  }

  @Nested
  @DisplayName("deleteGroup")
  class DeleteGroup {

    @Test
    @DisplayName("조를 삭제하면 조원은 미배정 상태가 된다")
    void deletesGroupAndUnassignsMembers() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User user = member("google-3", "c@getit.com", "경영학과");
      user.assignToGroup(group.getId());

      groupService.deleteGroup(group.getId());

      assertThat(groupRepository.findById(group.getId())).isEmpty();
      assertThat(userRepository.findById(user.getId()).orElseThrow().getGroupId()).isNull();
    }

    @Test
    @DisplayName("없는 조를 삭제하면 예외가 발생한다")
    void throwsWhenGroupNotFound() {
      assertThatThrownBy(() -> groupService.deleteGroup(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("addMembers")
  class AddMembers {

    @Test
    @DisplayName("미배정 사용자를 조에 추가한다")
    void addsUnassignedMembers() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User first = member("google-4", "d@getit.com", "경영학과");
      User second = member("google-5", "e@getit.com", "컴퓨터공학과");

      GroupMemberAddResult result = groupService.addMembers(
          group.getId(), List.of(first.getId(), second.getId()));

      // 원자적 UPDATE(assignToGroupIfUnassigned)로 반영되므로 영속성 컨텍스트가 비워진다.
      // first · second 는 이제 detached 라 groupId 를 다시 조회해서 확인해야 한다.
      assertThat(result.addedCount()).isEqualTo(2);
      assertThat(result.memberCount()).isEqualTo(2);
      assertThat(userRepository.findById(first.getId()).orElseThrow().getGroupId()).isEqualTo(group.getId());
      assertThat(userRepository.findById(second.getId()).orElseThrow().getGroupId()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("이미 이 조에 속한 사용자는 멱등하게 건너뛰고 addedCount 에 포함하지 않는다")
    void skipsMembersAlreadyInTargetGroup() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User already = member("google-11", "already@getit.com", "경영학과");
      already.assignToGroup(group.getId());
      User newMember = member("google-12", "new@getit.com", "컴퓨터공학과");

      GroupMemberAddResult result = groupService.addMembers(
          group.getId(), List.of(already.getId(), newMember.getId()));

      assertThat(result.addedCount()).isEqualTo(1);
      assertThat(result.memberCount()).isEqualTo(2);
      assertThat(userRepository.findById(newMember.getId()).orElseThrow().getGroupId()).isEqualTo(group.getId());
    }

    @Test
    @DisplayName("다른 기수 사용자를 추가하면 예외가 발생한다")
    void throwsWhenUserBelongsToOtherGeneration() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      User otherGenerationUser = userRepository.save(
          User.createGuest("google-13", "other-gen@getit.com", "부원", "url"));
      otherGenerationUser.promoteToMember(otherGeneration.getGenerationNo());

      assertThatThrownBy(() -> groupService.addMembers(group.getId(), List.of(otherGenerationUser.getId())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("탈퇴한 사용자를 추가하면 예외가 발생한다")
    void throwsWhenUserIsWithdrawn() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User withdrawn = member("google-14", "withdrawn@getit.com", "경영학과");
      withdrawn.withdraw();

      assertThatThrownBy(() -> groupService.addMembers(group.getId(), List.of(withdrawn.getId())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 다른 조에 속한 사용자가 있으면 전체가 거부된다")
    void rejectsWhenAnyAlreadyInGroup() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      Group otherGroup = groupRepository.save(Group.create(activeGeneration.getId(), "2조"));
      User already = member("google-6", "f@getit.com", "경영학과");
      already.assignToGroup(otherGroup.getId());
      User unassigned = member("google-7", "g@getit.com", "컴퓨터공학과");

      assertThatThrownBy(() -> groupService.addMembers(
          group.getId(), List.of(already.getId(), unassigned.getId())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.ALREADY_IN_GROUP);

      assertThat(unassigned.getGroupId()).isNull();
    }

    @Test
    @DisplayName("없는 사용자 id 가 섞여 있으면 예외가 발생한다")
    void throwsWhenUserNotFound() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User existing = member("google-8", "h@getit.com", "경영학과");

      assertThatThrownBy(() -> groupService.addMembers(
          group.getId(), List.of(existing.getId(), 999L)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 조에 추가하면 예외가 발생한다")
    void throwsWhenGroupNotFound() {
      User user = member("google-9", "i@getit.com", "경영학과");

      assertThatThrownBy(() -> groupService.addMembers(999L, List.of(user.getId())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GROUP_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("removeMember")
  class RemoveMember {

    @Test
    @DisplayName("조원을 빼면 미배정 상태가 된다")
    void removesMember() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      User user = member("google-10", "j@getit.com", "경영학과");
      user.assignToGroup(group.getId());

      groupService.removeMember(group.getId(), user.getId());

      assertThat(user.getGroupId()).isNull();
    }

    @Test
    @DisplayName("없는 조원이면 예외가 발생한다")
    void throwsWhenUserNotFound() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));

      assertThatThrownBy(() -> groupService.removeMember(group.getId(), 999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 조 소속 사용자를 빼려 하면 예외가 발생하고 원래 배정은 유지된다")
    void throwsWhenUserBelongsToDifferentGroup() {
      Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
      Group otherGroup = groupRepository.save(Group.create(activeGeneration.getId(), "2조"));
      User user = member("google-15", "k@getit.com", "경영학과");
      user.assignToGroup(otherGroup.getId());

      assertThatThrownBy(() -> groupService.removeMember(group.getId(), user.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.USER_NOT_FOUND);

      assertThat(user.getGroupId()).isEqualTo(otherGroup.getId());
    }
  }
}
