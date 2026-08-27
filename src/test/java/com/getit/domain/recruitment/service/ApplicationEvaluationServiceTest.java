package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.BulkDecisionResult;
import com.getit.domain.recruitment.dto.DocumentDecisionResult;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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
class ApplicationEvaluationServiceTest {

  @Autowired
  private ApplicationEvaluationService applicationEvaluationService;

  @Autowired
  private ApplicationRepository applicationRepository;

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

  private EvaluationCriterion criterion(Long generationId, int order, String name, int maxScore) {
    return evaluationCriterionRepository.save(
        EvaluationCriterion.create(generationId, order, name, name + " 가이드 라인", maxScore));
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

      EvaluationScoreSaveResult result = applicationEvaluationService.saveScores(
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

      EvaluationScoreSaveResult result = applicationEvaluationService.saveScores(
          application.getId(),
          List.of(new EvaluationScoreItem(first.getId(), 50), new EvaluationScoreItem(second.getId(), 30)));

      assertThat(result.totalScore()).isEqualTo(80);
    }

    @Test
    @DisplayName("이미 점수가 있으면 덮어쓰고 새로 만들지 않는다")
    void overwritesExistingScore() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);

      applicationEvaluationService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 30)));
      EvaluationScoreSaveResult result = applicationEvaluationService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 45)));

      assertThat(result.totalScore()).isEqualTo(45);
      assertThat(evaluationScoreRepository.findByApplicationId(application.getId())).hasSize(1);
    }

    @Test
    @DisplayName("점수가 배점을 초과하면 예외가 발생한다")
    void throwsWhenScoreExceedsMax() {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = criterion(activeGeneration.getId(), 1, "전공 적합성", 60);

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
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

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(otherCriterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 기준이면 예외가 발생한다")
    void throwsWhenCriterionNotFound() {
      Application application = submitted(1L, "홍길동");

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
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

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
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
      applicationEvaluationService.decide(application.getId(), true);

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_SCORABLE);
    }

    @Test
    @DisplayName("비활성 기수의 지원서는 채점할 수 없다 (존재하지 않는 것과 동일하게 취급)")
    void throwsWhenGenerationInactive() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Application application = applicationRepository.save(Application.createDraft(
          1L, otherGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678",
          null, null, 2, "2021110000"));
      application.submit(LocalDateTime.now());
      EvaluationCriterion criterion = criterion(otherGeneration.getId(), 1, "전공 적합성", 60);

      assertThatThrownBy(() -> applicationEvaluationService.saveScores(
          application.getId(), List.of(new EvaluationScoreItem(criterion.getId(), 10))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("decide")
  class Decide {

    @Test
    @DisplayName("true 면 DOC_PASS 로 결정한다")
    void decidesPass() {
      Application application = submitted(1L, "홍길동");

      DocumentDecisionResult result = applicationEvaluationService.decide(application.getId(), true);

      assertThat(result.status()).isEqualTo(ApplicationStatus.DOC_PASS);
    }

    @Test
    @DisplayName("false 면 DOC_FAIL 로 결정한다")
    void decidesFail() {
      Application application = submitted(1L, "홍길동");

      DocumentDecisionResult result = applicationEvaluationService.decide(application.getId(), false);

      assertThat(result.status()).isEqualTo(ApplicationStatus.DOC_FAIL);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 결정할 수 없다 (존재하지 않는 것과 동일하게 취급)")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");

      assertThatThrownBy(() -> applicationEvaluationService.decide(application.getId(), true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("DOC_PASS 상태에서 다시 결정하면 최종 합불(FINAL_PASS/FINAL_FAIL)로 전이한다 (7.4 확장)")
    void decidesFinalResultFromDocPass() {
      Application application = submitted(1L, "홍길동");
      applicationEvaluationService.decide(application.getId(), true); // SUBMITTED -> DOC_PASS

      DocumentDecisionResult result = applicationEvaluationService.decide(application.getId(), true);

      assertThat(result.status()).isEqualTo(ApplicationStatus.FINAL_PASS);
    }

    @Test
    @DisplayName("DOC_PASS 상태에서 false 로 결정하면 FINAL_FAIL 로 전이한다 (7.4 확장)")
    void decidesFinalFailFromDocPass() {
      Application application = submitted(1L, "홍길동");
      applicationEvaluationService.decide(application.getId(), true); // SUBMITTED -> DOC_PASS

      DocumentDecisionResult result = applicationEvaluationService.decide(application.getId(), false);

      assertThat(result.status()).isEqualTo(ApplicationStatus.FINAL_FAIL);
    }

    @Test
    @DisplayName("이미 최종 결정된(FINAL_PASS) 지원서를 다시 결정하려 하면 예외가 발생한다")
    void throwsWhenAlreadyFinalDecided() {
      Application application = submitted(1L, "홍길동");
      applicationEvaluationService.decide(application.getId(), true); // -> DOC_PASS
      applicationEvaluationService.decide(application.getId(), true); // -> FINAL_PASS

      assertThatThrownBy(() -> applicationEvaluationService.decide(application.getId(), true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    @Test
    @DisplayName("DOC_FAIL 상태의 지원서는 더 이상 결정할 수 없다 (다음 단계가 없음)")
    void throwsWhenDocFail() {
      Application application = submitted(1L, "홍길동");
      applicationEvaluationService.decide(application.getId(), false); // -> DOC_FAIL

      assertThatThrownBy(() -> applicationEvaluationService.decide(application.getId(), true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_SUBMITTED);
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationEvaluationService.decide(999L, true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 기수의 지원서는 결정할 수 없다 (존재하지 않는 것과 동일하게 취급)")
    void throwsWhenGenerationInactive() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Application application = applicationRepository.save(Application.createDraft(
          1L, otherGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678",
          null, null, 2, "2021110000"));
      application.submit(LocalDateTime.now());

      assertThatThrownBy(() -> applicationEvaluationService.decide(application.getId(), true))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("decideBulk (7.4 일괄 처리)")
  class DecideBulk {

    @Test
    @DisplayName("SUBMITTED 인 것만 DOC_PASS 로 일괄 갱신한다")
    void updatesOnlySubmittedToDocPass() {
      Application first = submitted(1L, "홍길동");
      Application second = submitted(2L, "김철수");
      Application alreadyDocPass = submitted(3L, "이영희");
      applicationEvaluationService.decide(alreadyDocPass.getId(), true);

      BulkDecisionResult result = applicationEvaluationService.decideBulk(
          List.of(first.getId(), second.getId(), alreadyDocPass.getId()), ApplicationStatus.DOC_PASS);

      assertThat(result.updatedCount()).isEqualTo(2);
      assertThat(result.status()).isEqualTo(ApplicationStatus.DOC_PASS);
    }

    @Test
    @DisplayName("DOC_PASS 인 것만 FINAL_PASS 로 일괄 갱신한다")
    void updatesOnlyDocPassToFinalPass() {
      Application docPass1 = submitted(1L, "홍길동");
      applicationEvaluationService.decide(docPass1.getId(), true);
      Application docPass2 = submitted(2L, "김철수");
      applicationEvaluationService.decide(docPass2.getId(), true);
      Application stillSubmitted = submitted(3L, "이영희");

      BulkDecisionResult result = applicationEvaluationService.decideBulk(
          List.of(docPass1.getId(), docPass2.getId(), stillSubmitted.getId()), ApplicationStatus.FINAL_PASS);

      assertThat(result.updatedCount()).isEqualTo(2);
      assertThat(applicationRepository.findById(stillSubmitted.getId()).orElseThrow().getStatus())
          .isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("대상이 없으면 updatedCount 0을 반환한다")
    void returnsZeroWhenNoMatch() {
      Application application = submitted(1L, "홍길동");

      BulkDecisionResult result = applicationEvaluationService.decideBulk(
          List.of(application.getId()), ApplicationStatus.FINAL_PASS);

      assertThat(result.updatedCount()).isZero();
    }

    @Test
    @DisplayName("목표 status 가 DRAFT · SUBMITTED 면 예외가 발생한다")
    void throwsWhenTargetStatusInvalid() {
      Application application = submitted(1L, "홍길동");

      assertThatThrownBy(() -> applicationEvaluationService.decideBulk(
          List.of(application.getId()), ApplicationStatus.SUBMITTED))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.INVALID_DECISION_STATUS);
    }

    @Test
    @DisplayName("비활성 기수의 지원서는 함께 갱신되지 않는다")
    void doesNotUpdateApplicationInInactiveGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Application otherGenerationApplication = applicationRepository.save(Application.createDraft(
          1L, otherGeneration.getId(), "지난기수", "old@gmail.com", "010-0000-0000",
          null, null, 2, null));
      otherGenerationApplication.submit(LocalDateTime.now());
      Application activeApplication = submitted(2L, "홍길동");

      BulkDecisionResult result = applicationEvaluationService.decideBulk(
          List.of(otherGenerationApplication.getId(), activeApplication.getId()), ApplicationStatus.DOC_PASS);

      assertThat(result.updatedCount()).isEqualTo(1);
      assertThat(applicationRepository.findById(otherGenerationApplication.getId()).orElseThrow().getStatus())
          .isEqualTo(ApplicationStatus.SUBMITTED);
    }
  }
}
