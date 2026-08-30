package com.getit.domain.setting.photo.exception;

import org.springframework.http.HttpStatus;

import com.getit.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityPhotoErrorCode implements ErrorCode {

  ACTIVITY_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "활동 사진을 찾을 수 없습니다."),
  NOT_PUBLIC_FILE(HttpStatus.BAD_REQUEST, "공개 저장소에 올린 파일만 사용할 수 있습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
