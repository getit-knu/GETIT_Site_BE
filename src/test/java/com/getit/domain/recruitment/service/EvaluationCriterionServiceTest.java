package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import com.getit.domain.recruitment.entity.EvaluationScore;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.EvaluationCriteriaSummary;
import com.getit.domain.recruitment.dto.EvaluationCriterionResult;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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
class EvaluationCriterionServiceTest {

  @Autowired
  private EvaluationCriterionService evaluationCriterionService;

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

  @Nested
  @DisplayName("getCriteria")
  class GetCriteria {

    @Test
    @DisplayName("기준이 없으면 빈 목록과 totalScore 0, valid false 를 반환한다")
    void returnsEmptySummary() {
      EvaluationCriteriaSummary summary = evaluationCriterionService.getCriteria();

      assertThat(summary.criteria()).isEmpty();
      assertThat(summary.totalScore()).isZero();
      assertThat(summary.valid()).isFalse();
    }

    @Test
    @DisplayName("배점 합계가 100 이면 valid 가 true 다")
    void validWhenTotalIs100() {
      evaluationCriterionService.createCriterion("전공 적합성", "가이드 라인", 60);
      evaluationCriterionService.createCriterion("지원 동기", "가이드 라인", 40);

      EvaluationCriteriaSummary summary = evaluationCriterionService.getCriteria();

      assertThat(summary.totalScore()).isEqualTo(100);
      assertThat(summary.valid()).isTrue();
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> evaluationCriterionService.getCriteria())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createCriterion")
  class CreateCriterion {

    @Test
    @DisplayName("order 가 1부터 순서대로 부여된다")
    void assignsOrderSequentially() {
      EvaluationCriterionResult first = evaluationCriterionService.createCriterion(
          "전공 적합성", "전공 적합성 가이드 라인", 20);
      EvaluationCriterionResult second = evaluationCriterionService.createCriterion(
          "지원 동기", "지원 동기 가이드 라인", 30);

      assertThat(first.order()).isEqualTo(1);
      assertThat(second.order()).isEqualTo(2);
      assertThat(second.maxScore()).isEqualTo(30);
    }

    @Test
    @DisplayName("배점 합계가 100 이하면 허용된다")
    void allowsExactly100() {
      evaluationCriterionService.createCriterion("전공 적합성", "가이드 라인", 70);

      EvaluationCriterionResult second = evaluationCriterionService.createCriterion(
          "지원 동기", "가이드 라인", 30);

      assertThat(second.maxScore()).isEqualTo(30);
      assertThat(evaluationCriterionService.getCriteria().totalScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("배점 합계가 100 을 초과하면 예외가 발생한다")
    void rejectsWhenTotalExceeds100() {
      evaluationCriterionService.createCriterion("전공 적합성", "가이드 라인", 70);

      assertThatThrownBy(() -> evaluationCriterionService.createCriterion(
          "지원 동기", "가이드 라인", 40))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.INVALID_CRITERIA_TOTAL);

      // 실패한 요청은 저장되지 않아야 한다.
      assertThat(evaluationCriterionService.getCriteria().criteria()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("updateCriterion")
  class UpdateCriterion {

    @Test
    @DisplayName("내용을 수정하고 order 는 유지한다")
    void updatesContentKeepsOrder() {
      EvaluationCriterionResult created = evaluationCriterionService.createCriterion(
          "전공 적합성", "원래 가이드 라인", 20);

      EvaluationCriterionResult updated = evaluationCriterionService.updateCriterion(
          created.id(), "전공 적합성(수정)", "수정된 가이드 라인", 25);

      assertThat(updated.order()).isEqualTo(created.order());
      assertThat(updated.name()).isEqualTo("전공 적합성(수정)");
      assertThat(updated.guideline()).isEqualTo("수정된 가이드 라인");
      assertThat(updated.maxScore()).isEqualTo(25);
    }

    @Test
    @DisplayName("없는 기준을 수정하면 예외가 발생한다")
    void throwsWhenCriterionNotFound() {
      assertThatThrownBy(() -> evaluationCriterionService.updateCriterion(
          999L, "이름", "가이드 라인", 20))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수가 아닌 기준을 수정하면 예외가 발생한다")
    void throwsWhenCriterionBelongsToInactiveGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      EvaluationCriterion otherCriterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(otherGeneration.getId(), 1, "지난 기수 기준", "가이드 라인", 20));

      assertThatThrownBy(() -> evaluationCriterionService.updateCriterion(
          otherCriterion.getId(), "이름", "가이드 라인", 20))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("자기 자신을 제외한 나머지와의 합만으로 검증한다")
    void excludesOwnScoreFromTotalCheck() {
      EvaluationCriterionResult first = evaluationCriterionService.createCriterion(
          "전공 적합성", "가이드 라인", 60);
      evaluationCriterionService.createCriterion("지원 동기", "가이드 라인", 40);

      // 나머지(40) + 자기 자신의 새 값(60) = 100, 통과해야 한다.
      EvaluationCriterionResult updated = evaluationCriterionService.updateCriterion(
          first.id(), "전공 적합성", "가이드 라인", 60);

      assertThat(updated.maxScore()).isEqualTo(60);
    }

    @Test
    @DisplayName("수정 후 배점 합계가 100 을 초과하면 예외가 발생한다")
    void rejectsWhenUpdatedTotalExceeds100() {
      EvaluationCriterionResult first = evaluationCriterionService.createCriterion(
          "전공 적합성", "가이드 라인", 60);
      evaluationCriterionService.createCriterion("지원 동기", "가이드 라인", 40);

      assertThatThrownBy(() -> evaluationCriterionService.updateCriterion(
          first.id(), "전공 적합성", "가이드 라인", 61))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.INVALID_CRITERIA_TOTAL);
    }
  }

  @Nested
  @DisplayName("deleteCriterion")
  class DeleteCriterion {


    @Test
    @DisplayName("그 기준으로 매긴 점수도 함께 삭제된다")
    void deletesScoresOfCriterion() {
      EvaluationCriterionResult target = evaluationCriterionService.createCriterion(
          "전공 적합성", "가이드 라인", 50);
      EvaluationCriterionResult kept = evaluationCriterionService.createCriterion(
          "지원 동기", "가이드 라인", 50);
      evaluationScoreRepository.save(EvaluationScore.create(1L, target.id(), 101L, 40));
      evaluationScoreRepository.save(EvaluationScore.create(1L, target.id(), 102L, 45));
      evaluationScoreRepository.save(EvaluationScore.create(1L, kept.id(), 101L, 30));

      evaluationCriterionService.deleteCriterion(target.id());

      // 남으면 어떤 조회로도 닿지 않는 행이 기수마다 쌓인다 (이슈 #157).
      assertThat(evaluationScoreRepository.findByApplicationId(1L))
          .extracting(EvaluationScore::getCriterionId)
          .containsExactly(kept.id());
    }

    @Test
    @DisplayName("점수가 없는 기준도 그냥 삭제된다")
    void deletesCriterionWithoutScores() {
      EvaluationCriterionResult created = evaluationCriterionService.createCriterion(
          "전공 적합성", "가이드 라인", 20);

      evaluationCriterionService.deleteCriterion(created.id());

      assertThat(evaluationCriterionRepository.findById(created.id())).isEmpty();
    }
    @Test
    @DisplayName("기준을 삭제한다")
    void deletesCriterion() {
      EvaluationCriterionResult created = evaluationCriterionService.createCriterion(
          "전공 적합성", "가이드 라인", 20);

      evaluationCriterionService.deleteCriterion(created.id());

      assertThat(evaluationCriterionRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("없는 기준을 삭제하면 예외가 발생한다")
    void throwsWhenCriterionNotFound() {
      assertThatThrownBy(() -> evaluationCriterionService.deleteCriterion(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수가 아닌 기준을 삭제하면 예외가 발생한다")
    void throwsWhenCriterionBelongsToInactiveGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      EvaluationCriterion otherCriterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(otherGeneration.getId(), 1, "지난 기수 기준", "가이드 라인", 20));

      assertThatThrownBy(() -> evaluationCriterionService.deleteCriterion(otherCriterion.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.CRITERION_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 후 뒤 순서의 기준들이 한 칸씩 당겨져서 order 중복이 생기지 않는다")
    void compactsOrderAfterDelete() {
      EvaluationCriterionResult first = evaluationCriterionService.createCriterion(
          "1번", "가이드 라인", 10);
      EvaluationCriterionResult second = evaluationCriterionService.createCriterion(
          "2번", "가이드 라인", 10);
      EvaluationCriterionResult third = evaluationCriterionService.createCriterion(
          "3번", "가이드 라인", 10);

      evaluationCriterionService.deleteCriterion(second.id());

      List<EvaluationCriterionResult> remaining = evaluationCriterionService.getCriteria().criteria();
      assertThat(remaining).extracting(EvaluationCriterionResult::id)
          .containsExactly(first.id(), third.id());
      assertThat(remaining).extracting(EvaluationCriterionResult::order)
          .containsExactly(1, 2);
    }
  }
}
