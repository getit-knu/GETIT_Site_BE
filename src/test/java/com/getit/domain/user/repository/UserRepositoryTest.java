package com.getit.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

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
}
