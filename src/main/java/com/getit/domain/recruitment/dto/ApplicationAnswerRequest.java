package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationAnswer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 답변 upsert 요청. (API 명세서 3.3 · 3.4)
 *
 * <p>임시 저장 단계에서는 <b>정책</b> 검증(필수 여부, 질문별 글자 수)을 하지 않는다. 쓰다 만
 * 상태를 그대로 담아야 하기 때문이다. 그 검증은 제출(3.4) 시점에 서비스 레이어에서 질문의
 * required · maxLength 를 참조해 수행한다.
 *
 * <p>다만 컬럼이 감당하는 <b>물리적</b> 상한은 임시 저장에서도 지켜야 한다. 없으면 긴 답변이
 * 검증을 통과해 그대로 TEXT 컬럼에 들어가다 500 이 난다 (이슈 #171).
 * {@link ApplicationAnswer#MAX_ANSWER_LENGTH} 참고.
 */
public record ApplicationAnswerRequest(
    @NotNull Long questionId,
    @Size(max = ApplicationAnswer.MAX_ANSWER_LENGTH) String answerText,
    List<String> selectedOptions
) { }
