package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.AdjacentApplicantResult;
import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.dto.DocumentDecisionResult;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
  private EvaluationCriterionRepository evaluationCriterionRepository;

  @Autowired
  private EvaluationScoreRepository evaluationScoreRepository;

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

  private Application submittedAt(Long userId, String name, LocalDateTime submittedAt) {
    Application application = draft(userId, name);
    application.submit(submittedAt);
    return application;
  }

  private EvaluationCriterion criterion(Long generationId, int order, String name, int maxScore) {
    return evaluationCriterionRepository.save(
        EvaluationCriterion.create(generationId, order, name, name + " 가이드 라인", maxScore));
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

  @Nested
  @DisplayName("saveScores")
  class SaveScores {

    @Test
    @DisplayName("채점되지 않은 기준이 남아있으면 총점은 null 이다")
    void totalScoreIsNullWhenNotAllCriteriaScored() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion scored = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);
      criterion(activeGeneration.getId(), 2, "지원 동기", 40);

      EvaluationScoreSaveResult result = applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(scored.getId(), 50)));

      assertThat(result.applicationId()).isEqualTo(application.getId());
      assertThat(result.totalScore()).isNull();
      assertThat(result.scores()).hasSize(2);
      assertThat(result.scores()).filteredOn(s -> s.criterionId().equals(scored.getId()))
          .extracting(s -> s.score()).containsExactly(50);
      assertThat(result.scores()).filteredOn(s -> !s.criterionId().equals(scored.getId()))
          .extracting(s -> s.score()).containsExactly((Integer) null);
    }

    @Test
    @DisplayName("기수의 모든 기준을 채점하면 총점을 합산해 반환한다")
    void totalScoreIsSumWhenAllCriteriaScored() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion first = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);
      EvaluationCriterion second = criterion(activeGeneration.getId(), 2, "지원 동기", 40);

      EvaluationScoreSaveResult result = applicationAdminService.saveScores(
          application.getId(),
          List.of(new EvaluationScoreItem(first.getId(), 50), new EvaluationScoreItem(second.getId(), 30)));

      assertThat(result.totalScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("이미 점수가 있으면 덮어쓰고 새로 만들지 않는다")
    void overwritesExistingScore() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);

      applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 30)));
      EvaluationScoreSaveResult result = applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 45)));

      assertThat(result.totalScore()).isEqualTo(45);
      assertThat(evaluationScoreRepository.findByApplicationId(application.getId())).hasSize(1);
    }

    @Test
    @DisplayName("점수가 배점을 초과하면 예외가 발생한다")
    void throwsWhenScoreExceedsMax() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);

      assertThatThrownBy(() -> applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 61))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.SCORE_EXCEEDS_MAX);
    }

    @Test
    @DisplayName("다른 기수의 평가 기준이면 예외가 발생한다")
    void throwsWhenCriterionBelongsToOtherGeneration() {
      Application application = submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      EvaluationCriterion otherCriterion = criterion(otherGeneration.getId(), 1, "지난 기수 기준", 60);

      assertThatThrownBy(() -> applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(otherCriterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 기준이면 예외가 발생한다")
    void throwsWhenCriterionNotFound() {
      Application application = submitted(1L, "홍길동");

      assertThatThrownBy(() -> applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(999L, 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 채점할 수 없다")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);

      assertThatThrownBy(() -> applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 합불이 결정된 지원서는 채점할 수 없다")
    void throwsWhenAlreadyDecided() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);
      applicationAdminService.decide(application.getId(), true);

      assertThatThrownBy(() -> applicationAdminService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }
  }

  @Nested
  @DisplayName("decide")
  class Decide {

    @Test
    @DisplayName("true 면 DOC_PASS 로 결정한다")
    void decidesPass() {
      Application application = submitted(1L, "홍길동");

      DocumentDecisionResult result = applicationAdminService.decide(application.getId(), true);

      assertThat(result.status()).isEqualTo(ApplicationStatus.DOC_PASS);
    }

    @Test
    @DisplayName("false 면 DOC_FAIL 로 결정한다")
    void decidesFail() {
      Application application = submitted(1L, "홍길동");

      DocumentDecisionResult result = applicationAdminService.decide(application.getId(), false);

      assertThat(result.status()).isEqualTo(ApplicationStatus.DOC_FAIL);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 결정할 수 없다 (존재하지 않는 것과 동일하게 취급)")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");

      assertThatThrownBy(() -> applicationAdminService.decide(application.getId(), true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 결정된 지원서면 예외가 발생한다")
    void throwsWhenAlreadyDecided() {
      Application application = submitted(1L, "홍길동");
      applicationAdminService.decide(application.getId(), true);

      assertThatThrownBy(() -> applicationAdminService.decide(application.getId(), false))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationAdminService.decide(999L, true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getAdjacentApplicants")
  class GetAdjacentApplicants {

    @Test
    @DisplayName("제출일시 내림차순 기준으로 이전 · 다음 지원서 id 를 반환한다")
    void returnsPreviousAndNext() {
      Application oldest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 0));
      Application middle = submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 11, 0));
      Application newest = submittedAt(3L, "이영희", LocalDateTime.of(2026, 9, 1, 12, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(middle.getId(), null, null);

      assertThat(result.previousApplicationId()).isEqualTo(newest.getId());
      assertThat(result.nextApplicationId()).isEqualTo(oldest.getId());
    }

    @Test
    @DisplayName("가장 최근(맨 앞) 지원서는 이전이 없다")
    void firstHasNoPrevious() {
      Application newest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 12, 0));
      submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 10, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(newest.getId(), null, null);

      assertThat(result.previousApplicationId()).isNull();
      assertThat(result.nextApplicationId()).isNotNull();
    }

    @Test
    @DisplayName("가장 오래된(맨 뒤) 지원서는 다음이 없다")
    void lastHasNoNext() {
      submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 12, 0));
      Application oldest = submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 10, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(oldest.getId(), null, null);

      assertThat(result.previousApplicationId()).isNotNull();
      assertThat(result.nextApplicationId()).isNull();
    }

    @Test
    @DisplayName("필터에 맞지 않는 지원서는 이전 · 다음 모두 null 이다")
    void bothNullWhenNotInFilteredList() {
      Application draftApplication = draft(1L, "홍길동");

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(draftApplication.getId(), null, null);

      assertThat(result.previousApplicationId()).isNull();
      assertThat(result.nextApplicationId()).isNull();
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationAdminService.getAdjacentApplicants(999L, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("exportApplicantsExcel")
  class ExportApplicantsExcel {

    @Test
    @DisplayName("DRAFT 를 제외한 지원자를 엑셀로 내보낸다")
    void exportsApplicantsExcludingDraft() throws IOException {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      byte[] excel = applicationAdminService.exportApplicantsExcel(null, null);

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getLastRowNum()).isEqualTo(1);
        Row dataRow = sheet.getRow(1);
        assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("홍길동");
      }
    }

    @Test
    @DisplayName("활성 기수가 없고 generationId 도 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationAdminService.exportApplicantsExcel(null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }
}
