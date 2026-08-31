package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.dto.GroupMemberResult;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 부원이 보는 내 조. (이슈 #148) */
@SpringBootTest
@Transactional
class MemberGroupServiceTest {

  @Autowired
  private MemberGroupService memberGroupService;

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

  private User member(String providerId, String name) {
    User user = userRepository.save(
        User.createGuest(providerId, providerId + "@getit.com", name, "https://cdn.getit.com/1.png"));
    user.promoteToMember(activeGeneration.getGenerationNo());
    return user;
  }

  @Test
  @DisplayName("배정된 조의 이름과 조원을 함께 준다")
  void returnsGroupWithMembers() {
    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
    User me = member("google-sub-me", "나");
    User teammate = member("google-sub-teammate", "동료");
    me.assignToGroup(group.getId());
    teammate.assignToGroup(group.getId());
    userRepository.flush();

    GroupWithMembersResult result = memberGroupService.findMyGroup(me.getId()).orElseThrow();

    assertThat(result.name()).isEqualTo("1조");
    assertThat(result.memberCount()).isEqualTo(2);
    assertThat(result.members()).extracting(GroupMemberResult::name)
        .containsExactlyInAnyOrder("나", "동료");
  }

  @Test
  @DisplayName("아직 배정되지 않았으면 비어 있다 — 오류가 아니다")
  void returnsEmptyWhenNotAssigned() {
    User me = member("google-sub-unassigned", "나");
    userRepository.flush();

    // 조 배정은 어드민이 한다. 배정 전인 부원이 있는 것은 정상이다.
    assertThat(memberGroupService.findMyGroup(me.getId())).isEmpty();
  }

  @Test
  @DisplayName("다른 조의 조원은 섞이지 않는다")
  void doesNotMixOtherGroups() {
    Group mine = groupRepository.save(Group.create(activeGeneration.getId(), "1조"));
    Group other = groupRepository.save(Group.create(activeGeneration.getId(), "2조"));
    User me = member("google-sub-mine", "나");
    User stranger = member("google-sub-other", "남");
    me.assignToGroup(mine.getId());
    stranger.assignToGroup(other.getId());
    userRepository.flush();

    GroupWithMembersResult result = memberGroupService.findMyGroup(me.getId()).orElseThrow();

    assertThat(result.members()).extracting(GroupMemberResult::name).containsExactly("나");
  }

  @Test
  @DisplayName("탈퇴한 사용자면 USER_NOT_FOUND")
  void throwsWhenUserWithdrawn() {
    User me = member("google-sub-gone", "나");
    me.withdraw();
    userRepository.flush();

    assertThatThrownBy(() -> memberGroupService.findMyGroup(me.getId()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("없는 사용자면 USER_NOT_FOUND")
  void throwsWhenUserMissing() {
    Optional<Long> missingId = Optional.of(999_999L);

    assertThatThrownBy(() -> memberGroupService.findMyGroup(missingId.orElseThrow()))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_FOUND);
  }
}
