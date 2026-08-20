package com.getit.domain.recruitment.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 지원서 임시 저장 · 제출 요청. (API 명세서 3.3 · 3.4 — "3.4 요청 본문은 3.3과 동일 구조")
 *
 * <p>3.4(제출)는 본문 없이 저장된 draft 를 그대로 제출하는 것도 허용하므로, 컨트롤러에서
 * {@code required = false} 로 받아 이 레코드 자체가 {@code null} 일 수 있다. {@code basicInfo}·
 * {@code answers} 자체엔 애너테이션 검증을 달지 않는 이유는 {@link ApplicationAnswerRequest} 참고 —
 * 임시 저장 단계에서는 필수값 · 글자수 검증을 하지 않는다. {@code answers} 원소의
 * {@code questionId} {@code @NotNull} 만 {@code @Valid} 로 계단식 검증되도록 한다.
 */
public record ApplicationDraftRequest(
    BasicInfo basicInfo,
    @Valid List<ApplicationAnswerRequest> answers
) { }
