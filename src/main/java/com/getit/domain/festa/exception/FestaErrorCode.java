package com.getit.domain.festa.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 모의투자 FESTA 에러 코드. (이슈 #222) */
@Getter
@RequiredArgsConstructor
public enum FestaErrorCode implements ErrorCode {

  FESTA_CLOSED(HttpStatus.CONFLICT, "모의투자 기간이 아닙니다."),
  FESTA_RESET_WHILE_OPEN(HttpStatus.CONFLICT, "모의투자가 열려 있는 동안에는 투자 데이터를 초기화할 수 없습니다."),
  INVALID_FESTA_SCHEDULE(HttpStatus.BAD_REQUEST, "시작 시각은 종료 시각보다 앞서야 합니다."),
  DUPLICATE_PROJECT(HttpStatus.BAD_REQUEST, "같은 프로젝트를 두 번 넣을 수 없습니다."),
  INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "투자금은 0 이상, 10만 원 단위여야 합니다."),
  BUDGET_EXCEEDED(HttpStatus.BAD_REQUEST, "투자금 합계가 가상 자금을 넘을 수 없습니다."),
  PROJECT_NOT_ENTERED(HttpStatus.BAD_REQUEST, "출전하지 않은 프로젝트에는 투자할 수 없습니다."),
  OWN_GROUP_PROJECT(HttpStatus.FORBIDDEN, "우리 조 프로젝트에는 투자할 수 없습니다."),
  INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "전화번호는 010-0000-0000 형식이어야 합니다."),
  PHONE_NUMBER_REQUIRED(HttpStatus.BAD_REQUEST, "추첨용 전화번호가 필요합니다."),
  PRIVACY_CONSENT_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집·이용에 동의해야 투자할 수 있습니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
