package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationAnswer;
import java.util.List;

/** 지원서 답변 조회 결과. (API 명세서 3.2) */
public record ApplicationAnswerResult(
    Long questionId,
    String answerText,
    List<String> selectedOptions
) {

  public static ApplicationAnswerResult from(ApplicationAnswer answer) {
    return new ApplicationAnswerResult(
        answer.getQuestionId(),
        answer.getAnswerText(),
        answer.getSelectedOptions()
    );
  }
}
