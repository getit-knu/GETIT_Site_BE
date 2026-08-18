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
  SUBCATEGORY_TRACK_MISMATCH(HttpStatus.BAD_REQUEST, "소분류가 해당 트랙에 속하지 않습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() { return name(); }
}
