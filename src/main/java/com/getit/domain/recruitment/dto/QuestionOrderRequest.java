package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 질문 순서 변경 요청. (API 명세서 6.7) 배열 인덱스 순서대로 order 를 1부터 재부여한다. */
public record QuestionOrderRequest(
    @NotEmpty List<Long> orderedIds
) { }
