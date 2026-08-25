package com.getit.domain.recruitment.dto;

import java.util.List;

/**
 * 서류 평가 점수 저장 결과. (API 명세서 7.3)
 *
 * <p>{@code scores} 는 기수의 평가 기준 전체 기준으로 반환한다 — 요청에 없던 기준도 {@code score}
 * 가 null 인 채로 포함해서, 아직 다 채점되지 않았는지 클라이언트가 판단할 수 있게 한다.
 */
public record EvaluationScoreSaveResult(
    Long applicationId,
    List<EvaluationScoreResult> scores,
    int totalScore
) { }
