package com.getit.domain.qna.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** Q&A 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum QnaErrorCode implements ErrorCode {

  QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
  ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
  ALREADY_ANSWERED(HttpStatus.CONFLICT, "이미 답변이 등록된 질문입니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
