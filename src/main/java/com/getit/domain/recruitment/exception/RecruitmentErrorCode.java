package com.getit.domain.recruitment.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 모집 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum RecruitmentErrorCode implements ErrorCode {

  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),
  SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "모집 일정을 찾을 수 없습니다."),
  QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문 항목을 찾을 수 없습니다."),
  CRITERION_NOT_FOUND(HttpStatus.NOT_FOUND, "평가 기준을 찾을 수 없습니다."),
  INVALID_CRITERIA_TOTAL(HttpStatus.BAD_REQUEST, "평가 기준 배점 합계는 100점을 초과할 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
