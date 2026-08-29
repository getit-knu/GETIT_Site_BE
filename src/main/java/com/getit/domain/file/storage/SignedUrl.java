package com.getit.domain.file.storage;

/**
 * 읽기 주소와 그 주소가 실제로 유효한 시간.
 *
 * <p>만료 시간을 설정에서 따로 읽으면 저장소 구현과 어긋난다. 로컬 저장소는 만료가 없는
 * 정적 주소를 주는데 응답에는 "5분" 이라고 적히는 식이다. 주소를 만든 쪽이 만료도 함께 말한다.
 *
 * @param url 읽기 주소
 * @param expiresInSeconds 유효 시간. 만료가 없으면 {@link #NEVER_EXPIRES}
 */
public record SignedUrl(String url, int expiresInSeconds) {

  /** 만료가 없는 주소. 정적 서빙이 여기 해당한다. */
  public static final int NEVER_EXPIRES = 0;

  public static SignedUrl permanent(String url) {
    return new SignedUrl(url, NEVER_EXPIRES);
  }
}
