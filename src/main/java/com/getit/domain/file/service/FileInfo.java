package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.StorageVisibility;
import com.getit.domain.file.storage.SignedUrl;

/**
 * 다른 도메인에 넘기는 파일 정보.
 *
 * @param url 지금 바로 열 수 있는 주소. 비공개 저장소면 짧게 사는 서명 주소이고,
 *            공개 저장소면 고정 주소다. {@code FileAsset.url} 의 고정 주소는
 *            비공개 컨테이너에서 그대로는 열리지 않으므로 쓰지 않는다.
 * @param publiclyReadable 공개 저장소에 있어 고정 주소로 열리는 파일인지.
 *                         공개 화면에 붙일 파일인지 판단할 때 쓴다
 * @param urlExpiresInSeconds {@code url} 이 실제로 유효한 시간.
 *                            만료가 없으면 {@link SignedUrl#NEVER_EXPIRES}.
 *                            소비자가 응답에 적어 내려줄 때 이 값을 쓴다 — 설정에서
 *                            따로 읽거나 상수로 박으면 실제 서명 만료와 어긋난다.
 */
public record FileInfo(
    Long fileId,
    String url,
    int urlExpiresInSeconds,
    boolean publiclyReadable,
    String originalName,
    String contentType,
    Long size,
    Long uploaderId
) {

  public static FileInfo from(FileAsset file, SignedUrl signed) {
    return new FileInfo(
        file.getId(), signed.url(), signed.expiresInSeconds(), isPublic(file),
        file.getOriginalName(), file.getContentType(), file.getSize(), file.getUploaderId());
  }

  /**
   * 공개 저장소에 있는 파일인지.
   *
   * <p>저장 키가 {@code public/} 로 시작하면 공개다. 용도(purpose)를 따로 저장하지 않으므로
   * 키 접두어가 유일한 단서다 — 라우팅도 같은 접두어를 본다.
   */
  private static boolean isPublic(FileAsset file) {
    return file.getStoredKey().startsWith(StorageVisibility.PUBLIC.getKeyPrefix() + "/");
  }
}
