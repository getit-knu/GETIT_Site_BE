package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** lecture 가 소비하는 부원 조회 계약. (작업 분할 계획 4.2, 이슈 #30) */
@SpringBootTest
@Transactional
class UserQueryServiceImplTest {

  @Autowired
  private UserQueryService userQueryService;

  @Autowired
  private UserRepository userRepository;

  private User memberOf(int generationNo, String providerId, String email, String major) {
    User user = User.createGuest(providerId, email, "김부원", null);
    user.updateApplicantInfo(null, null, major, null, null);
    user.promoteToMember(generationNo);
    return userRepository.save(user);
  }

  @Test
  @DisplayName("특정 기수의 활성 부원만 MemberSummary 로 반환한다")
  void returnsActiveMembersOfGeneration() {
    User member = memberOf(9, "google-sub-20", "m@getit.com", "경영학과");
    memberOf(8, "google-sub-21", "n@getit.com", "컴퓨터공학과");

    User withdrawn = memberOf(9, "google-sub-22", "o@getit.com", "경제학과");
    withdrawn.withdraw();
    userRepository.flush();

    List<MemberSummary> members = userQueryService.findActiveMembers(9);

    assertThat(members).hasSize(1);
    assertThat(members.get(0).userId()).isEqualTo(member.getId());
    assertThat(members.get(0).userName()).isEqualTo("김부원");
    assertThat(members.get(0).major()).isEqualTo("경영학과");
    assertThat(members.get(0).groupId()).isNull();
  }

  @Test
  @DisplayName("조에 배정된 부원은 groupId 를 함께 반환한다")
  void includesGroupIdWhenAssigned() {
    User member = memberOf(9, "google-sub-23", "p@getit.com", "경영학과");
    member.assignToGroup(1L);
    userRepository.flush();

    List<MemberSummary> members = userQueryService.findActiveMembers(9);

    assertThat(members).hasSize(1);
    assertThat(members.get(0).groupId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("해당 기수에 활성 부원이 없으면 빈 리스트를 반환한다")
  void returnsEmptyWhenNoActiveMembers() {
    assertThat(userQueryService.findActiveMembers(9)).isEmpty();
  }
}
