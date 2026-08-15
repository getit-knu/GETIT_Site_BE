package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicationQuestionResult;
import com.getit.domain.recruitment.dto.QuestionOption;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 지원서 질문 항목 조회 · 설정. (API 명세서 6.3 · 6.4 · 6.5 · 6.6 · 6.7) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationQuestionService {

  /** TEXT 질문의 기본 글자 수 제한. (API 명세서 6.4) */
  private static final int DEFAULT_MAX_LENGTH = 300;

  private final ApplicationQuestionRepository applicationQuestionRepository;
  private final GenerationQueryService generationQueryService;

  public List<ApplicationQuestionResult> getQuestions() {
    GenerationSummary activeGeneration = findActiveGeneration();

    return applicationQuestionRepository.findByGenerationId(activeGeneration.id()).stream()
        .map(ApplicationQuestionResult::from)
        .toList();
  }

  @Transactional
  public ApplicationQuestionResult createQuestion(
      QuestionType type, String content, boolean required, Integer maxLength, List<QuestionOption> options
  ) {
    validateOptions(type, options);

    GenerationSummary activeGeneration = findActiveGeneration();
    int nextOrder = (int) applicationQuestionRepository.countByGenerationId(activeGeneration.id()) + 1;

    ApplicationQuestion saved = applicationQuestionRepository.save(
        ApplicationQuestion.create(
            activeGeneration.id(), nextOrder, type, content, required,
            resolveMaxLength(type, maxLength), options));

    return ApplicationQuestionResult.from(saved);
  }

  @Transactional
  public ApplicationQuestionResult updateQuestion(
      Long questionId, QuestionType type, String content, boolean required, Integer maxLength,
      List<QuestionOption> options
  ) {
    validateOptions(type, options);

    ApplicationQuestion question = findQuestion(questionId);
    question.update(type, content, required, resolveMaxLength(type, maxLength), options);

    return ApplicationQuestionResult.from(question);
  }

  @Transactional
  public void deleteQuestion(Long questionId) {
    applicationQuestionRepository.delete(findQuestion(questionId));
  }

  /** 6.7. 배열 인덱스 순서대로 order 를 1부터 재부여한다. */
  @Transactional
  public void reorderQuestions(List<Long> orderedIds) {
    for (int i = 0; i < orderedIds.size(); i++) {
      findQuestion(orderedIds.get(i)).updateOrder(i + 1);
    }
  }

  private ApplicationQuestion findQuestion(Long questionId) {
    return applicationQuestionRepository.findById(questionId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.QUESTION_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  /** CHOICE · CHECKBOX 는 options 가 2개 이상 필요하다. (API 명세서 6.4) */
  private void validateOptions(QuestionType type, List<QuestionOption> options) {
    if ((type == QuestionType.CHOICE || type == QuestionType.CHECKBOX)
        && (options == null || options.size() < 2)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "선택형 질문은 옵션이 2개 이상 필요합니다.");
    }
  }

  /** TEXT 가 아니면 maxLength 는 의미가 없어 null 로 둔다. 지정하지 않으면 기본값을 채운다. */
  private Integer resolveMaxLength(QuestionType type, Integer maxLength) {
    if (type != QuestionType.TEXT) {
      return null;
    }
    return maxLength != null ? maxLength : DEFAULT_MAX_LENGTH;
  }
}
