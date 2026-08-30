package com.getit.domain.recruitment.dto;

import java.util.List;

/**
 * 한 운영진이 한 지원서에 매긴 점수 묶음. (7.3)
 *
 * <p>{@code applicationId} 와 {@code evaluatorId} 는 둘 다 {@code Long} 이라 인자로 나란히
 * 두면 순서를 바꿔도 컴파일된다. 다른 지원서·평가자의 점수를 건드리는 실수가 조용히
 * 지나가므로 이름 있는 값으로 묶는다 (PR #154 Copilot 리뷰 지적).
 */
public record EvaluationSubmission(
    Long applicationId,
    Long evaluatorId,
    List<EvaluationScoreItem> items
) { }
