package com.getit.domain.setting.home.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 홈 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum HomeErrorCode implements ErrorCode {

  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),

  /** 요청 바디의 generation.generationNo 가 현재 활성 기수와 다를 때. */
  GENERATION_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "요청한 기수가 활성 기수가 아닙니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
