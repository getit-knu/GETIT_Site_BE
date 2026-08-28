package com.getit.domain.lecture.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LectureErrorCode implements ErrorCode {

  LECTURE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),
  GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),
  TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "트랙을 찾을 수 없습니다."),
  SUBCATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "소분류를 찾을 수 없습니다."),
  SUBCATEGORY_TRACK_MISMATCH(HttpStatus.BAD_REQUEST, "소분류가 해당 트랙에 속하지 않습니다."),
  ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "과제를 찾을 수 없습니다."),
  SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "제출물을 찾을 수 없습니다."),
  DUPLICATE_SUBMISSION(HttpStatus.CONFLICT, "이미 제출한 과제입니다."),
  SUBMISSION_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "파일 또는 링크 중 하나는 반드시 제출해야 합니다."),
  SUBMISSION_TYPE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "이 과제가 허용하지 않는 제출 방식입니다."),
  INVALID_LINK_FORMAT(HttpStatus.BAD_REQUEST, "올바르지 않은 링크 형식입니다."),
  LINK_HOST_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "허용되지 않은 링크 호스트입니다."),
  FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "피드백을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() { return name(); }
}
