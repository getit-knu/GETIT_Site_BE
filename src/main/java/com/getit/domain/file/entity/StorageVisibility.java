package com.getit.domain.file.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 파일을 어느 저장소에 둘지. 저장 키 접두어로 드러난다. */
@Getter
@RequiredArgsConstructor
public enum StorageVisibility {

  /** 누구나 URL 로 읽는다. 고정 주소라 캐시가 걸린다. */
  PUBLIC("public"),

  /** 권한을 확인한 뒤 짧게 사는 서명 주소로만 읽는다. */
  PRIVATE("private");

  private final String keyPrefix;
}
