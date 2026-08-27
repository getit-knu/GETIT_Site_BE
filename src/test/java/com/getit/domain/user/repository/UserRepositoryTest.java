package com.getit.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 엔티티 매핑과 제약이 실제 DB 스키마로 반영되는지 검증한다.
 *
 * <p>@DataJpaTest 는 임의의 @Configuration 을 로드하지 않으므로 JpaAuditingConfig 를 직접 넣는다.
 * 없으면 createdAt 이 null 로 남아 nullable=false 제약에 걸린다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  private User guest(String providerId, String email) {
    return User.createGuest(providerId, email, "김부원", "https://cdn.getit.com/1.png");
  }

  @Test
  @DisplayName("사용자를 저장하면 감사 컬럼이 채워진다")
  void fillsAuditingColumns() {
    User saved = userRepository.save(guest("google-sub-1", "a@getit.com"));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();
    assertThat(saved.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("providerId 로 조회한다")
  void findsByProviderId() {
    userRepository.save(guest("google-sub-2", "b@getit.com"));

    assertThat(userRepository.findByProviderId("google-sub-2"))
        .isPresent()
        .get()
        .extracting(User::getEmail)
        .isEqualTo("b@getit.com");
  }

  @Test
  @DisplayName("없는 providerId 는 빈 Optional 을 반환한다")
  void returnsEmptyForUnknownProviderId() {
    assertThat(userRepository.findByProviderId("google-sub-없음")).isEmpty();
  }

  @Test
  @DisplayName("email 로 조회하고 존재 여부를 확인한다")
  void findsByEmail() {
    userRepository.save(guest("google-sub-3", "c@getit.com"));

    assertThat(userRepository.findByEmail("c@getit.com")).isPresent();
    assertThat(userRepository.existsByEmail("c@getit.com")).isTrue();
    assertThat(userRepository.existsByEmail("none@getit.com")).isFalse();
  }

  @Test
  @DisplayName("email 이 중복되면 저장에 실패한다")
  void rejectsDuplicateEmail() {
    userRepository.saveAndFlush(guest("google-sub-4", "dup@getit.com"));

    assertThatThrownBy(() -> userRepository.saveAndFlush(guest("google-sub-5", "dup@getit.com")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("providerId 가 중복되면 저장에 실패한다")
  void rejectsDuplicateProviderId() {
    userRepository.saveAndFlush(guest("google-sub-6", "e@getit.com"));

    assertThatThrownBy(() -> userRepository.saveAndFlush(guest("google-sub-6", "f@getit.com")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("10자리 학번을 포함한 지원서 정보가 저장된다")
  void persistsApplicantInfo() {
    User user = userRepository.save(guest("google-sub-7", "g@getit.com"));

    user.updateApplicantInfo("010-1234-5678", "경영대학", "경영학과", 3, "2021110000");
    userRepository.flush();

    User found = userRepository.findById(user.getId()).orElseThrow();
    assertThat(found.getStudentNumber()).isEqualTo("2021110000");
    assertThat(found.getPhoneNumber()).isEqualTo("010-1234-5678");
    assertThat(found.getCollege()).isEqualTo("경영대학");
    assertThat(found.getMajor()).isEqualTo("경영학과");
    assertThat(found.getStudentYear()).isEqualTo(3);
  }

  @Test
  @DisplayName("탈퇴 상태가 DB 에 반영된다")
  void persistsWithdrawal() {
    User user = userRepository.save(guest("google-sub-8", "h@getit.com"));

    user.withdraw();
    userRepository.flush();

    User found = userRepository.findById(user.getId()).orElseThrow();
    assertThat(found.isDeleted()).isTrue();
    assertThat(found.getRole()).isEqualTo(Role.GUEST);
  }

  @Test
  @DisplayName("특정 기수의 활성 부원만 조회한다")
  void findsActiveMembersByGenerationNo() {
    User member9 = userRepository.save(guest("google-sub-9", "i@getit.com"));
    member9.promoteToMember(9);

    User otherGeneration = userRepository.save(guest("google-sub-10", "j@getit.com"));
    otherGeneration.promoteToMember(8);

    userRepository.save(guest("google-sub-11", "k@getit.com"));

    User withdrawnMember = userRepository.save(guest("google-sub-12", "l@getit.com"));
    withdrawnMember.promoteToMember(9);
    withdrawnMember.withdraw();

    userRepository.flush();

    assertThat(userRepository.findByRoleAndStatusAndGenerationNo(Role.MEMBER, UserStatus.ACTIVE, 9))
        .extracting(User::getEmail)
        .containsExactly("i@getit.com");
  }

  @Test
  @DisplayName("특정 기수의 활성 사용자 전체를 조 배정 여부와 무관하게 조회한다")
  void findsAllActiveUsersByGenerationNoRegardlessOfGroup() {
    User grouped = userRepository.save(guest("google-sub-13", "m@getit.com"));
    grouped.promoteToMember(9);
    grouped.assignToGroup(1L);

    User unassigned = userRepository.save(guest("google-sub-14", "n@getit.com"));
    unassigned.promoteToMember(9);

    User otherGeneration = userRepository.save(guest("google-sub-15", "o@getit.com"));
    otherGeneration.promoteToMember(8);

    User withdrawnMember = userRepository.save(guest("google-sub-16", "p@getit.com"));
    withdrawnMember.promoteToMember(9);
    withdrawnMember.withdraw();

    userRepository.flush();

    assertThat(userRepository.findByGenerationNoAndStatus(9, UserStatus.ACTIVE))
        .extracting(User::getEmail)
        .containsExactlyInAnyOrder("m@getit.com", "n@getit.com");
  }

  @Test
  @DisplayName("미배정 사용자만 원자적으로 조에 배정하고 반영된 행 수를 반환한다")
  void assignsOnlyUnassignedUsersAtomically() {
    User unassigned = userRepository.save(guest("google-sub-17", "q@getit.com"));
    User alreadyGrouped = userRepository.save(guest("google-sub-18", "r@getit.com"));
    alreadyGrouped.assignToGroup(2L);
    userRepository.flush();

    int updated = userRepository.assignToGroupIfUnassigned(
        1L, List.of(unassigned.getId(), alreadyGrouped.getId()));

    assertThat(updated).isEqualTo(1);
    assertThat(userRepository.findById(unassigned.getId()).orElseThrow().getGroupId()).isEqualTo(1L);
    // 이미 배정돼 있던 사용자는 건드리지 않는다 (다른 조에 이미 속한 경우를 덮어쓰지 않아야 함)
    assertThat(userRepository.findById(alreadyGrouped.getId()).orElseThrow().getGroupId()).isEqualTo(2L);
  }

  @Nested
  @DisplayName("searchUsers")
  class SearchUsers {

    private Page<User> search(String keyword, Role role, Integer generationNo, Long groupId, boolean unassignedOnly) {
      return userRepository.searchUsers(
          keyword, role, generationNo, groupId, unassignedOnly, PageRequest.of(0, 20));
    }

    @Test
    @DisplayName("이름 또는 이메일에 keyword 가 포함된 사용자를 대소문자 구분 없이 찾는다")
    void filtersByKeyword() {
      userRepository.save(User.createGuest("google-20", "kim@getit.com", "김부원", "url"));
      userRepository.save(User.createGuest("google-21", "MATCH@getit.com", "이회원", "url"));
      userRepository.save(User.createGuest("google-22", "c@getit.com", "박학생", "url"));

      assertThat(search("김", null, null, null, false).getContent())
          .extracting(User::getName).containsExactly("김부원");
      assertThat(search("match", null, null, null, false).getContent())
          .extracting(User::getEmail).containsExactly("MATCH@getit.com");
    }

    @Test
    @DisplayName("role 로 필터링한다")
    void filtersByRole() {
      User admin = userRepository.save(User.createGuest("google-23", "d@getit.com", "운영진", "url"));
      admin.updateRole(Role.ADMIN);
      userRepository.save(User.createGuest("google-24", "e@getit.com", "게스트", "url"));

      assertThat(search(null, Role.ADMIN, null, null, false).getContent())
          .extracting(User::getEmail).containsExactly("d@getit.com");
    }

    @Test
    @DisplayName("generationNo 로 필터링한다")
    void filtersByGenerationNo() {
      User user9 = userRepository.save(User.createGuest("google-25", "f@getit.com", "9기", "url"));
      user9.promoteToMember(9);
      User user8 = userRepository.save(User.createGuest("google-26", "g@getit.com", "8기", "url"));
      user8.promoteToMember(8);

      assertThat(search(null, null, 9, null, false).getContent())
          .extracting(User::getEmail).containsExactly("f@getit.com");
    }

    @Test
    @DisplayName("groupId 로 특정 조만 필터링한다")
    void filtersByGroupId() {
      User grouped = userRepository.save(User.createGuest("google-27", "h@getit.com", "1조원", "url"));
      grouped.assignToGroup(1L);
      User otherGrouped = userRepository.save(User.createGuest("google-28", "i@getit.com", "2조원", "url"));
      otherGrouped.assignToGroup(2L);

      assertThat(search(null, null, null, 1L, false).getContent())
          .extracting(User::getEmail).containsExactly("h@getit.com");
    }

    @Test
    @DisplayName("unassignedOnly 면 미배정 사용자만 조회한다")
    void filtersUnassignedOnly() {
      User grouped = userRepository.save(User.createGuest("google-29", "j@getit.com", "조원", "url"));
      grouped.assignToGroup(1L);
      userRepository.save(User.createGuest("google-30", "k@getit.com", "미배정", "url"));

      assertThat(search(null, null, null, null, true).getContent())
          .extracting(User::getEmail).containsExactly("k@getit.com");
    }

    @Test
    @DisplayName("필터가 전혀 없으면 전체를 페이지네이션으로 반환한다")
    void returnsAllWhenNoFilter() {
      userRepository.save(User.createGuest("google-31", "l2@getit.com", "부원1", "url"));
      userRepository.save(User.createGuest("google-32", "m2@getit.com", "부원2", "url"));

      Page<User> page = search(null, null, null, null, false);

      assertThat(page.getTotalElements()).isEqualTo(2);
    }
  }
}
