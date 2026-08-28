package com.getit.domain.setting.feature.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FeatureErrorCode implements ErrorCode {

  FEATURE_NOT_FOUND(HttpStatus.NOT_FOUND, "기능 토글을 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
