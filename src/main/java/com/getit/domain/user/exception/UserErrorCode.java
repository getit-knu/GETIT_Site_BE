package com.getit.domain.user.exception;

import com.getit.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 사용자 도메인 에러 코드. (API 명세서 0.4) */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
  GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "조를 찾을 수 없습니다."),
  DUPLICATE_GROUP_NAME(HttpStatus.CONFLICT, "같은 기수 안에 이미 같은 이름의 조가 있습니다."),
  ALREADY_IN_GROUP(HttpStatus.CONFLICT, "이미 다른 조에 속한 사용자입니다."),
  GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "기수를 찾을 수 없습니다."),
  ACTIVE_GENERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 기수가 없습니다."),
  CANNOT_REMOVE_OWN_ADMIN(HttpStatus.FORBIDDEN, "자기 자신의 ADMIN 권한은 해제할 수 없습니다."),
  INVALID_GROUP_FILTER(HttpStatus.BAD_REQUEST, "groupId 는 숫자 또는 'none' 이어야 합니다."),
  GROUP_GENERATION_MISMATCH(HttpStatus.BAD_REQUEST, "조의 소속 기수와 사용자의 소속 기수가 다릅니다."),
  NOT_PUBLIC_PROFILE_IMAGE(HttpStatus.BAD_REQUEST, "프로필 사진은 공개 저장소의 파일이어야 합니다."),
  GROUP_ASSIGN_CONFLICT(HttpStatus.BAD_REQUEST, "조 배정과 해제를 함께 요청할 수 없습니다."),
  COLLEGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 단과대학입니다."),
  MAJOR_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 학과입니다."),
  MAJOR_NOT_IN_COLLEGE(HttpStatus.BAD_REQUEST, "선택한 단과대학에 속한 학과가 아닙니다."),
  AFFILIATION_INCOMPLETE(HttpStatus.BAD_REQUEST, "단과대학과 학과는 함께 보내야 합니다.");

  private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
