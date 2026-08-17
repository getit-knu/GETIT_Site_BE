package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import java.util.List;

/**
 * 지원서 양식의 질문 항목. (API 명세서 3.1)
 *
 * <p>{@code ApplicationQuestionResult}(6.3 관리자 조회)와 필드는 겹치지만, 지원자 화면 전용
 * {@code placeholder} 가 있어 별도 record 로 둔다. {@code ApplicationQuestion} 에는 아직
 * placeholder 컬럼이 없어 항상 {@code null} 이다 (이슈 #38 논의 필요 사항 참고).
 */
public record ApplicationFormQuestion(
    Long id,
    Integer order,
    QuestionType type,
    String content,
    String placeholder,
    boolean required,
    Integer maxLength,
    List<QuestionOption> options
) {

  public static ApplicationFormQuestion from(ApplicationQuestion question) {
    return new ApplicationFormQuestion(
        question.getId(),
        question.getOrder(),
        question.getType(),
        question.getContent(),
        null,
        question.isRequired(),
        question.getMaxLength(),
        question.getOptions()
    );
  }
}
