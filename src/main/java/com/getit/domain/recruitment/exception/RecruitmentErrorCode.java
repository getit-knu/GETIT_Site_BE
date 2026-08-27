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
  INVALID_CRITERIA_TOTAL(HttpStatus.BAD_REQUEST, "평가 기준 배점 합계는 100점을 초과할 수 없습니다."),
  APPLICATION_NOT_OPEN(HttpStatus.UNPROCESSABLE_ENTITY, "모집 기간이 아닙니다."),
  APPLICATION_DEADLINE_PASSED(HttpStatus.UNPROCESSABLE_ENTITY, "지원서 제출 기한이 지났습니다."),
  ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 지원서입니다."),
  REQUIRED_ANSWER_MISSING(HttpStatus.BAD_REQUEST, "필수 질문에 답변하지 않았습니다."),
  ANSWER_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "답변이 글자 수 제한을 초과했습니다."),
  BASIC_INFO_INCOMPLETE(HttpStatus.BAD_REQUEST, "이름 · 이메일 · 연락처 · 단과대학 · 전공 · 학년을 모두 입력해야 합니다."),
  APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "지원서를 찾을 수 없습니다."),
  SCORE_EXCEEDS_MAX(HttpStatus.BAD_REQUEST, "점수가 평가 기준의 배점을 초과했습니다."),
  APPLICATION_NOT_SUBMITTED(HttpStatus.CONFLICT, "제출됨 또는 서류합격 상태의 지원서만 결정할 수 있습니다."),
  APPLICATION_NOT_SCORABLE(HttpStatus.CONFLICT, "제출된 지원서만 채점할 수 있습니다."),
  INVALID_DECISION_STATUS(HttpStatus.BAD_REQUEST, "status 는 DOC_PASS · DOC_FAIL · FINAL_PASS · FINAL_FAIL 중 하나여야 합니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
