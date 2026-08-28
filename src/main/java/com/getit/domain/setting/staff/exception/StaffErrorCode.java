package com.getit.domain.setting.staff.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 운영진 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum StaffErrorCode implements ErrorCode {

  STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "운영진을 찾을 수 없습니다."),
  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),

  /** 요청 바디의 generationNo 가 현재 활성 기수와 다를 때. */
  GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),

  /**
   * 10.22 순서 변경 요청에 중복된 id 가 있을 때. {@code CommonErrorCode.VALIDATION_FAILED} 를
   * 재사용하면 다른 검증 실패와 같은 code 가 되어 클라이언트가 원인을 구분할 수 없다
   * (PR #82 Copilot 리뷰 지적).
   */
  DUPLICATE_ORDER_ID(HttpStatus.BAD_REQUEST, "순서 변경 요청에 중복된 운영진 id 가 있습니다."),

  /** 10.22 순서 변경 요청이 해당 section 소속 운영진 전체를 빠짐없이 포함하지 않을 때. */
  INCOMPLETE_ORDER_SET(HttpStatus.BAD_REQUEST, "해당 section 의 운영진 전체를 빠짐없이 보내야 합니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
