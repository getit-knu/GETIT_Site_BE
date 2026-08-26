package com.getit.domain.recruitment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 서류 평가 점수 저장 요청. (API 명세서 7.3) */
public record EvaluationScoreSaveRequest(
    @NotEmpty @Valid List<EvaluationScoreItem> scores
) { }
