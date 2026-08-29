package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.storage.SignedUrl;

/**
 * 다른 도메인에 넘기는 파일 정보.
 *
 * @param url 지금 바로 열 수 있는 주소. 비공개 저장소면 짧게 사는 서명 주소이고,
 *            공개 저장소면 고정 주소다. {@code FileAsset.url} 의 고정 주소는
 *            비공개 컨테이너에서 그대로는 열리지 않으므로 쓰지 않는다.
 * @param urlExpiresInSeconds {@code url} 이 실제로 유효한 시간.
 *                            만료가 없으면 {@link SignedUrl#NEVER_EXPIRES}.
 *                            소비자가 응답에 적어 내려줄 때 이 값을 쓴다 — 설정에서
 *                            따로 읽거나 상수로 박으면 실제 서명 만료와 어긋난다.
 */
public record FileInfo(
    Long fileId,
    String url,
    int urlExpiresInSeconds,
    String originalName,
    String contentType,
    Long size,
    Long uploaderId
) {

  public static FileInfo from(FileAsset file, SignedUrl signed) {
    return new FileInfo(
        file.getId(), signed.url(), signed.expiresInSeconds(), file.getOriginalName(),
        file.getContentType(), file.getSize(), file.getUploaderId());
  }
}
