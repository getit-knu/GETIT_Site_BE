package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.dto.PromotionSkipReason;
import com.getit.domain.user.dto.PromotionSkipResult;
import com.getit.domain.user.dto.UserPromotionResult;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
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
class UserPromotionServiceTest {

  @Autowired
  private UserPromotionService userPromotionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private MajorRepository majorRepository;

  private Generation generation;

  @BeforeEach
  void setUpGeneration() {
    generation = Generation.create(9, 2026);
    generation.activate();
    generation = generationRepository.save(generation);
  }

  private User guest(String providerId, String email) {
    return userRepository.save(User.createGuest(providerId, email, "지원자", "url"));
  }

  private Application finalPassApplication(Long userId, Long collegeId, Long majorId) {
    Application application = applicationRepository.save(Application.createDraft(
        userId, generation.getId(), "지원자", "app@getit.com", "010-1234-5678",
        collegeId, majorId, 3, "2021110000"));
    application.submit(LocalDateTime.now());
    application.decideDocumentResult(true);
    application.decideFinalResult(true);
    return application;
  }

  @Nested
  @DisplayName("promote")
  class Promote {

    @Test
    @DisplayName("applicationIds 없이 호출하면 기수의 FINAL_PASS 전체를 승격한다")
    void promotesAllFinalPassInGeneration() {
      User user = guest("google-1", "a@getit.com");
      Application application = finalPassApplication(user.getId(), null, null);

      UserPromotionResult result = userPromotionService.promote(generation.getId(), null);

      assertThat(result.promotedCount()).isEqualTo(1);
      assertThat(result.skippedCount()).isZero();
      User promoted = userRepository.findById(user.getId()).orElseThrow();
      assertThat(promoted.getRole()).isEqualTo(Role.MEMBER);
      assertThat(promoted.getGenerationNo()).isEqualTo(9);
      assertThat(promoted.getPhoneNumber()).isEqualTo("010-1234-5678");
      assertThat(promoted.getStudentYear()).isEqualTo(3);
      assertThat(promoted.getStudentNumber()).isEqualTo("2021110000");
      assertThat(application).isNotNull();
    }

    @Test
    @DisplayName("collegeId·majorId 가 있으면 이름으로 변환해서 복사한다")
    void resolvesCollegeAndMajorNames() {
      College college = collegeRepository.save(College.create("IT융합대학"));
      Major major = majorRepository.save(Major.create(college.getId(), "컴퓨터공학과"));
      User user = guest("google-2", "b@getit.com");
      finalPassApplication(user.getId(), college.getId(), major.getId());

      userPromotionService.promote(generation.getId(), null);

      User promoted = userRepository.findById(user.getId()).orElseThrow();
      assertThat(promoted.getCollege()).isEqualTo("IT융합대학");
      assertThat(promoted.getMajor()).isEqualTo("컴퓨터공학과");
    }

    @Test
    @DisplayName("applicationIds 를 지정하면 그 중 FINAL_PASS 인 것만 승격한다")
    void promotesOnlySpecifiedIds() {
      User target = guest("google-3", "c@getit.com");
      Application targetApplication = finalPassApplication(target.getId(), null, null);
      User other = guest("google-4", "d@getit.com");
      finalPassApplication(other.getId(), null, null);

      UserPromotionResult result = userPromotionService.promote(
          generation.getId(), List.of(targetApplication.getId()));

      assertThat(result.promotedCount()).isEqualTo(1);
      assertThat(userRepository.findById(target.getId()).orElseThrow().getRole()).isEqualTo(Role.MEMBER);
      assertThat(userRepository.findById(other.getId()).orElseThrow().getRole()).isEqualTo(Role.GUEST);
    }

    @Test
    @DisplayName("이미 MEMBER 인 사용자는 ALREADY_MEMBER 로 건너뛴다")
    void skipsAlreadyMember() {
      User user = guest("google-5", "e@getit.com");
      user.promoteToMember(8);
      Application application = finalPassApplication(user.getId(), null, null);

      UserPromotionResult result = userPromotionService.promote(generation.getId(), null);

      assertThat(result.promotedCount()).isZero();
      assertThat(result.skipped()).containsExactly(
          new PromotionSkipResult(application.getId(), PromotionSkipReason.ALREADY_MEMBER));
    }

    @Test
    @DisplayName("탈퇴한 사용자는 USER_WITHDRAWN 으로 건너뛴다")
    void skipsWithdrawnUser() {
      User user = guest("google-6", "f@getit.com");
      Application application = finalPassApplication(user.getId(), null, null);
      user.withdraw();

      UserPromotionResult result = userPromotionService.promote(generation.getId(), null);

      assertThat(result.promotedCount()).isZero();
      assertThat(result.skipped()).containsExactly(
          new PromotionSkipResult(application.getId(), PromotionSkipReason.USER_WITHDRAWN));
    }

    @Test
    @DisplayName("applicationIds 로 지정했지만 FINAL_PASS 가 아니면 NOT_FINAL_PASS 로 건너뛴다")
    void skipsNonFinalPassId() {
      User user = guest("google-7", "g@getit.com");
      Application notFinalPass = applicationRepository.save(Application.createDraft(
          user.getId(), generation.getId(), "지원자", "app@getit.com", "010-1234-5678",
          null, null, 3, "2021110000"));
      notFinalPass.submit(LocalDateTime.now());

      UserPromotionResult result = userPromotionService.promote(
          generation.getId(), List.of(notFinalPass.getId()));

      assertThat(result.promotedCount()).isZero();
      assertThat(result.skipped()).containsExactly(
          new PromotionSkipResult(notFinalPass.getId(), PromotionSkipReason.NOT_FINAL_PASS));
    }

    @Test
    @DisplayName("대상이 없으면 promotedCount 0을 반환한다")
    void returnsZeroWhenNoCandidates() {
      UserPromotionResult result = userPromotionService.promote(generation.getId(), null);

      assertThat(result.promotedCount()).isZero();
      assertThat(result.skippedCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 generationId 면 예외가 발생한다")
    void throwsWhenGenerationNotFound() {
      assertThatThrownBy(() -> userPromotionService.promote(999L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수가 아닌 generationId 면 예외가 발생한다")
    void throwsWhenGenerationNotActive() {
      Generation inactiveGeneration = generationRepository.save(Generation.create(8, 2025));

      assertThatThrownBy(() -> userPromotionService.promote(inactiveGeneration.getId(), null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      generation.deactivate();

      assertThatThrownBy(() -> userPromotionService.promote(generation.getId(), null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(UserErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }
}
