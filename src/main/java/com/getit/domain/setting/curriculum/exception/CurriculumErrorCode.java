package com.getit.domain.setting.curriculum.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 커리큘럼 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum CurriculumErrorCode implements ErrorCode {

  CURRICULUM_NOT_FOUND(HttpStatus.NOT_FOUND, "커리큘럼을 찾을 수 없습니다."),
  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),

  /** 요청 바디의 generationId 가 현재 활성 기수와 다를 때. */
  GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
