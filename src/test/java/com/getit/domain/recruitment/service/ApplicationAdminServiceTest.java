package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
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
class ApplicationAdminServiceTest {

  @Autowired
  private ApplicationAdminService applicationAdminService;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private Application draft(Long userId, String name) {
    return applicationRepository.save(Application.createDraft(
        userId, activeGeneration.getId(), name, name + "@gmail.com", "010-1234-5678",
        null, null, 2, "2021110000"));
  }

  private Application submitted(Long userId, String name) {
    Application application = draft(userId, name);
    application.submit(LocalDateTime.now());
    return application;
  }

  @Nested
  @DisplayName("listApplicants")
  class ListApplicants {

    @Test
    @DisplayName("status 필터가 없으면 DRAFT 를 제외한 활성 기수 지원자를 반환한다")
    void excludesDraftWhenNoStatusFilter() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("홍길동");
      assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("status 필터가 있으면 해당 상태의 지원자만 반환한다")
    void filtersByStatus() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, ApplicationStatus.DRAFT, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("김철수");
    }

    @Test
    @DisplayName("generationId 가 없으면 활성 기수만 조회하고 다른 기수는 포함하지 않는다")
    void excludesOtherGenerationWhenNoGenerationIdGiven() {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난 기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("홍길동");
    }

    @Test
    @DisplayName("generationId 를 지정하면 해당 기수(비활성 포함) 지원자를 조회한다")
    void filtersByExplicitGenerationId() {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      Application otherGenApplication = applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난 기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));
      otherGenApplication.submit(LocalDateTime.now());

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(otherGeneration.getId(), null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("지난 기수");
    }

    @Test
    @DisplayName("generationId 가 없고 활성 기수도 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getApplicantDetail")
  class GetApplicantDetail {

    @Test
    @DisplayName("기본 정보와 답변을 함께 반환한다")
    void returnsDetailWithAnswers() {
      Application application = submitted(1L, "홍길동");
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      ApplicantDetailResult result = applicationAdminService.getApplicantDetail(application.getId());

      assertThat(result.id()).isEqualTo(application.getId());
      assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
      assertThat(result.basicInfo().name()).isEqualTo("홍길동");
      assertThat(result.basicInfo().studentNumber()).isEqualTo("2021110000");
      assertThat(result.answers()).hasSize(1);
      assertThat(result.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("지난 기수 지원자도 조회할 수 있다")
    void returnsDetailForInactiveGeneration() {
      Application application = submitted(1L, "홍길동");
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThat(applicationAdminService.getApplicantDetail(application.getId()).id())
          .isEqualTo(application.getId());
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationAdminService.getApplicantDetail(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 조회할 수 없다")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");

      assertThatThrownBy(() -> applicationAdminService.getApplicantDetail(application.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }
}
