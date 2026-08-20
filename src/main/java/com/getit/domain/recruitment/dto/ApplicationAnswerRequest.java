package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 답변 upsert 요청. (API 명세서 3.3 · 3.4)
 *
 * <p>임시 저장 단계에서는 필수 · 글자수 검증을 하지 않으므로 {@code answerText}·
 * {@code selectedOptions} 에는 별도 검증 애너테이션을 달지 않는다 — 검증은 제출(3.4) 시점에
 * 서비스 레이어에서 질문의 required · maxLength 를 참조해 수행한다.
 */
public record ApplicationAnswerRequest(
    @NotNull Long questionId,
    String answerText,
    List<String> selectedOptions
) { }
