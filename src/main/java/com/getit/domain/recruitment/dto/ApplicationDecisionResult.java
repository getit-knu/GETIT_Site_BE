package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;

/**
 * 지원서 결과 조회. (API 명세서 3.5)
 *
 * <p>{@code documentAnnouncedAt}/{@code finalAnnouncedAt} 은 {@code RecruitmentSchedule} 에
 * 별도 "발표일" 필드가 없어 각각 {@code documentEndAt}/{@code interviewEndAt}(=totalEndAt) 으로
 * 근사한다 (이슈 #44 논의 필요 사항 참고). {@code nextStep} 은 {@code DOC_PASS} 일 때만 채운다.
 */
public record ApplicationDecisionResult(
    Integer generationNo,
    ApplicationStatus status,
    String statusLabel,
    LocalDateTime documentAnnouncedAt,
    LocalDateTime finalAnnouncedAt,
    NextStep nextStep
) { }
