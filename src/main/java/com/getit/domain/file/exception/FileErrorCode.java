package com.getit.domain.file.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileErrorCode implements ErrorCode {

  FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
  INVALID_FILE_EXTENSION(HttpStatus.BAD_REQUEST, "허용되지 않은 확장자입니다."),
  INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "파일 용량 제한을 초과했습니다."),
  FILE_IN_USE(HttpStatus.CONFLICT, "다른 도메인에서 사용 중인 파일입니다."),
  FILE_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 다른 리소스에 연결된 파일입니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() { return name(); }
}
