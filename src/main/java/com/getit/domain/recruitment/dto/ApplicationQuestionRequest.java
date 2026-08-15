package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** 질문 추가 · 수정 요청. (API 명세서 6.4 · 6.5) */
public record ApplicationQuestionRequest(
    @NotNull QuestionType type,
    @NotBlank String content,
    Boolean required,
    Integer maxLength,
    List<QuestionOption> options
) {

  /** required 는 명세서 기본값이 false 다. */
  public boolean requiredOrDefault() {
    return Boolean.TRUE.equals(required);
  }
}
