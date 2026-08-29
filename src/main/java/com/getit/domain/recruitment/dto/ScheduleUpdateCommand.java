package com.getit.domain.recruitment.dto;

import java.time.LocalDateTime;

/**
 * 모집 일정 갱신 명령. (API 명세서 6.2, 10.20 홈 일괄 저장 소비용)
 *
 * <p>5개의 연속된 {@code LocalDateTime} 인자를 그대로 주고받으면 순서를 잘못 넘기는 실수를
 * 컴파일 타임에 잡을 수 없다({@code UpdateGenerationCommand}와 동일한 이유로 record 로 감싼다).
 */
public record ScheduleUpdateCommand(
    LocalDateTime totalStartAt,
    LocalDateTime totalEndAt,
    LocalDateTime documentStartAt,
    LocalDateTime documentEndAt,
    LocalDateTime interviewStartAt
) { }
