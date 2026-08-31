package com.getit.domain.recruitment.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 지원서 임시 저장 · 제출 요청. (API 명세서 3.3 · 3.4 — "3.4 요청 본문은 3.3과 동일 구조")
 *
 * <p>3.4(제출)는 본문 없이 저장된 draft 를 그대로 제출하는 것도 허용하므로, 컨트롤러에서
 * {@code required = false} 로 받아 이 레코드 자체가 {@code null} 일 수 있다.
 *
 * <p>{@code basicInfo}·{@code answers} 자체엔 애너테이션 검증을 달지 않는다 — 임시 저장
 * 단계에서는 <b>정책</b> 검증(필수값, 질문별 글자 수)을 하지 않기 때문이다
 * ({@link ApplicationAnswerRequest} 참고).
 *
 * <p>{@code answers} 원소는 {@code @Valid} 로 계단식 검증된다. 지금 걸리는 것은
 * {@code questionId} 의 {@code @NotNull} 과, {@code answerText} 가 컬럼 용량을 넘지 않는지다
 * ({@link com.getit.domain.recruitment.entity.ApplicationAnswer#MAX_ANSWER_LENGTH}, 이슈 #171).
 * 뒤쪽은 정책이 아니라 무엇을 설정하든 넘을 수 없는 물리적 상한이라 임시 저장에도 건다.
 */
public record ApplicationDraftRequest(
    BasicInfo basicInfo,
    @Valid List<ApplicationAnswerRequest> answers
) { }
