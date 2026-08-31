package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 질문 추가 · 수정 요청. (API 명세서 6.4 · 6.5)
 *
 * @param content 컬럼이 {@code varchar(500)} 이다. 상한이 없으면 저장 단계에서 500 이 난다
 * @param maxLength 답변 글자 수 제한. 답변 컬럼이 담을 수 있는 크기를 넘겨 설정하면, 그 값을
 *                  지킨 답변조차 저장하지 못한다 — 제출 검증은 통과하고 DB 에서 터진다.
 *                  {@link ApplicationAnswer#MAX_ANSWER_LENGTH} 로 막는다 (이슈 #171)
 */
public record ApplicationQuestionRequest(
    @NotNull QuestionType type,
    @NotBlank @Size(max = 500) String content,
    Boolean required,
    @Min(1) @Max(ApplicationAnswer.MAX_ANSWER_LENGTH) Integer maxLength,
    List<QuestionOption> options
) {

  /** required 는 명세서 기본값이 false 다. */
  public boolean requiredOrDefault() {
    return Boolean.TRUE.equals(required);
  }
}
