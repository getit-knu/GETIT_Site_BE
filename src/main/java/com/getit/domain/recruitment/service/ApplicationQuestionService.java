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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    GenerationSummary activeGeneration = findActiveGeneration();
    ApplicationQuestion question = findQuestion(questionId, activeGeneration.id());
    question.update(type, content, required, resolveMaxLength(type, maxLength), options);

    return ApplicationQuestionResult.from(question);
  }

  /** 6.6. 삭제 후 뒤 순서를 한 칸씩 당겨서 order 중복(예: 3번 삭제 후 4번이 2개)을 막는다. */
  @Transactional
  public void deleteQuestion(Long questionId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    ApplicationQuestion question = findQuestion(questionId, activeGeneration.id());
    int deletedOrder = question.getOrder();

    applicationQuestionRepository.delete(question);

    applicationQuestionRepository.findByGenerationId(activeGeneration.id()).stream()
        .filter(q -> q.getOrder() > deletedOrder)
        .forEach(q -> q.updateOrder(q.getOrder() - 1));
  }

  /**
   * 6.7. 배열 인덱스 순서대로 order 를 1부터 재부여한다.
   *
   * <p>활성 기수의 질문 전체가, 중복 없이, 정확히 한 번씩 포함되어 있어야 한다. 일부만 보내거나
   * (나머지 order 와 충돌), id 가 중복되거나, 다른 기수의 질문 id 가 섞여 있으면 거부한다 (#33 리뷰).
   */
  @Transactional
  public void reorderQuestions(List<Long> orderedIds) {
    if (new HashSet<>(orderedIds).size() != orderedIds.size()) {
      throw new BusinessException(CommonErrorCode.VALIDATION_FAILED, "중복된 질문 id 가 있습니다.");
    }

    GenerationSummary activeGeneration = findActiveGeneration();
    List<ApplicationQuestion> questions =
        applicationQuestionRepository.findByGenerationId(activeGeneration.id());
    Map<Long, ApplicationQuestion> questionsById =
        questions.stream().collect(Collectors.toMap(ApplicationQuestion::getId, Function.identity()));

    Set<Long> orderedIdSet = new HashSet<>(orderedIds);
    if (orderedIds.size() != questions.size() || !questionsById.keySet().equals(orderedIdSet)) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "활성 기수의 질문 전체를 빠짐없이 보내야 합니다.");
    }

    for (int i = 0; i < orderedIds.size(); i++) {
      questionsById.get(orderedIds.get(i)).updateOrder(i + 1);
    }
  }

  /**
   * id 와 활성 기수 소속 여부를 함께 확인한다. 다른 기수(이미 지원서가 제출된 기수 포함)의 질문은
   * 수정 · 삭제 · 재정렬 대상에서 제외해야 기존 답변이 orphan 상태가 되는 것을 막을 수 있다 (#33 리뷰).
   */
  private ApplicationQuestion findQuestion(Long questionId, Long activeGenerationId) {
    return applicationQuestionRepository.findByIdAndGenerationId(questionId, activeGenerationId)
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
