package com.getit.domain.setting.event.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode implements ErrorCode {

  EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "행사를 찾을 수 없습니다."),
  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),
  GENERATION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "요청한 기수가 활성 기수가 아닙니다."),
  INVALID_EVENT_PERIOD(HttpStatus.BAD_REQUEST, "행사 종료일이 시작일보다 빠릅니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
