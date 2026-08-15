package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.ApplicationQuestionResult;
import com.getit.domain.recruitment.dto.QuestionOption;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
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
class ApplicationQuestionServiceTest {

  @Autowired
  private ApplicationQuestionService applicationQuestionService;

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private List<QuestionOption> options() {
    return List.of(new QuestionOption("sw", "SW 개발"), new QuestionOption("startup", "창업"));
  }

  @Nested
  @DisplayName("getQuestions")
  class GetQuestions {

    @Test
    @DisplayName("질문이 없으면 빈 리스트를 반환한다")
    void returnsEmptyList() {
      assertThat(applicationQuestionService.getQuestions()).isEmpty();
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationQuestionService.getQuestions())
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createQuestion")
  class CreateQuestion {

    @Test
    @DisplayName("주관식 질문은 maxLength 기본값 300 이 채워지고 order 가 1부터 부여된다")
    void createsTextQuestionWithDefaultMaxLength() {
      ApplicationQuestionResult first = applicationQuestionService.createQuestion(
          QuestionType.TEXT, "지원 동기", true, null, null);
      ApplicationQuestionResult second = applicationQuestionService.createQuestion(
          QuestionType.TEXT, "경험", false, null, null);

      assertThat(first.order()).isEqualTo(1);
      assertThat(first.maxLength()).isEqualTo(300);
      assertThat(second.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("체크박스 질문은 options 를 그대로 저장하고 maxLength 는 null 이다")
    void createsCheckboxQuestion() {
      ApplicationQuestionResult result = applicationQuestionService.createQuestion(
          QuestionType.CHECKBOX, "관심 트랙", true, null, options());

      assertThat(result.options()).containsExactlyElementsOf(options());
      assertThat(result.maxLength()).isNull();
    }

    @Test
    @DisplayName("선택형 질문에 options 가 2개 미만이면 검증 실패한다")
    void rejectsTooFewOptions() {
      assertThatThrownBy(() -> applicationQuestionService.createQuestion(
          QuestionType.CHOICE, "질문", true, null, List.of(new QuestionOption("a", "A"))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }
  }

  @Nested
  @DisplayName("updateQuestion")
  class UpdateQuestion {

    @Test
    @DisplayName("내용을 수정하고 order 는 유지한다")
    void updatesContentKeepsOrder() {
      ApplicationQuestionResult created = applicationQuestionService.createQuestion(
          QuestionType.TEXT, "원래 내용", false, 300, null);

      ApplicationQuestionResult updated = applicationQuestionService.updateQuestion(
          created.id(), QuestionType.TEXT, "수정된 내용", true, 200, null);

      assertThat(updated.order()).isEqualTo(created.order());
      assertThat(updated.content()).isEqualTo("수정된 내용");
      assertThat(updated.required()).isTrue();
      assertThat(updated.maxLength()).isEqualTo(200);
    }

    @Test
    @DisplayName("없는 질문을 수정하면 예외가 발생한다")
    void throwsWhenQuestionNotFound() {
      assertThatThrownBy(() -> applicationQuestionService.updateQuestion(
          999L, QuestionType.TEXT, "내용", false, 300, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.QUESTION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteQuestion")
  class DeleteQuestion {

    @Test
    @DisplayName("질문을 삭제한다")
    void deletesQuestion() {
      ApplicationQuestionResult created = applicationQuestionService.createQuestion(
          QuestionType.TEXT, "내용", false, 300, null);

      applicationQuestionService.deleteQuestion(created.id());

      assertThat(applicationQuestionRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("없는 질문을 삭제하면 예외가 발생한다")
    void throwsWhenQuestionNotFound() {
      assertThatThrownBy(() -> applicationQuestionService.deleteQuestion(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.QUESTION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("reorderQuestions")
  class ReorderQuestions {

    @Test
    @DisplayName("배열 순서대로 order 를 1부터 재부여한다")
    void reassignsOrder() {
      ApplicationQuestion q1 = applicationQuestionRepository.save(
          ApplicationQuestion.create(activeGeneration.getId(), 1, QuestionType.TEXT, "1번", false, 300, null));
      ApplicationQuestion q2 = applicationQuestionRepository.save(
          ApplicationQuestion.create(activeGeneration.getId(), 2, QuestionType.TEXT, "2번", false, 300, null));
      ApplicationQuestion q3 = applicationQuestionRepository.save(
          ApplicationQuestion.create(activeGeneration.getId(), 3, QuestionType.TEXT, "3번", false, 300, null));

      applicationQuestionService.reorderQuestions(List.of(q3.getId(), q1.getId(), q2.getId()));

      assertThat(applicationQuestionRepository.findById(q3.getId()).orElseThrow().getOrder()).isEqualTo(1);
      assertThat(applicationQuestionRepository.findById(q1.getId()).orElseThrow().getOrder()).isEqualTo(2);
      assertThat(applicationQuestionRepository.findById(q2.getId()).orElseThrow().getOrder()).isEqualTo(3);
    }
  }
}
