package com.getit.domain.setting.generation.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 기수 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum GenerationErrorCode implements ErrorCode {

  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),

  /**
   * 활성 기수 단일성은 DB 제약으로 표현할 수 없어(설계 명세서 4.5) 서비스가 원자적 조건부
   * UPDATE 로 보장한다. 두 요청이 동시에 활성화를 시도하면 뒤에 반영된 요청은 이 코드로 실패한다.
   */
  ACTIVE_GENERATION_EXISTS(HttpStatus.CONFLICT, "다른 요청이 먼저 기수를 활성화했습니다. 다시 시도해주세요.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
