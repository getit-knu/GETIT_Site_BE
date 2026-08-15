package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import java.util.List;

/** 지원서 질문 항목 조회 · 저장 결과. (API 명세서 6.3 · 6.4 · 6.5) */
public record ApplicationQuestionResult(
    Long id,
    Integer order,
    QuestionType type,
    String content,
    boolean required,
    Integer maxLength,
    List<QuestionOption> options
) {

  public static ApplicationQuestionResult from(ApplicationQuestion question) {
    return new ApplicationQuestionResult(
        question.getId(),
        question.getOrder(),
        question.getType(),
        question.getContent(),
        question.isRequired(),
        question.getMaxLength(),
        question.getOptions()
    );
  }
}
