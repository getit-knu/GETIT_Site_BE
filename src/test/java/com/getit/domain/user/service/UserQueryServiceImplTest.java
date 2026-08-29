package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
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

  @Test
  @DisplayName("countActiveMembers 는 기수 무관 전체 활성 부원 수를 센다")
  void countsActiveMembersAcrossGenerations() {
    memberOf(9, "google-sub-30", "q@getit.com", "경영학과");
    memberOf(8, "google-sub-31", "r@getit.com", "컴퓨터공학과");

    assertThat(userQueryService.countActiveMembers()).isEqualTo(2);
  }

  @Test
  @DisplayName("countActiveMembersInGeneration 은 해당 기수의 활성 부원 수만 센다")
  void countsActiveMembersOfGeneration() {
    memberOf(9, "google-sub-32", "s@getit.com", "경영학과");
    memberOf(8, "google-sub-33", "t@getit.com", "컴퓨터공학과");

    assertThat(userQueryService.countActiveMembersInGeneration(9)).isEqualTo(1);
  }

  @Test
  @DisplayName("findNamesByIds 는 id 로 이름을 일괄 조회한다")
  void returnsNamesByIds() {
    User member = memberOf(9, "google-sub-34", "u@getit.com", "경영학과");

    Map<Long, String> names = userQueryService.findNamesByIds(List.of(member.getId()));

    assertThat(names).containsEntry(member.getId(), "김부원");
  }

  @Test
  @DisplayName("findNamesByIds 는 빈 컬렉션이면 조회 없이 빈 맵을 반환한다")
  void returnsEmptyMapForEmptyIds() {
    assertThat(userQueryService.findNamesByIds(List.of())).isEmpty();
  }
}
