package com.getit.domain.user.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 사용자 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "조를 찾을 수 없습니다."),
  DUPLICATE_GROUP_NAME(HttpStatus.CONFLICT, "같은 기수 안에 이미 같은 이름의 조가 있습니다."),
  ALREADY_IN_GROUP(HttpStatus.CONFLICT, "이미 다른 조에 속한 사용자입니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
