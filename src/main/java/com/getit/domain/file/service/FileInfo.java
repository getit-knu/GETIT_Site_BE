package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;

public record FileInfo(
    Long fileId,
    String url,
    String originalName,
    String contentType,
    Long size,
    Long uploaderId
) {

  /**
   * @param readableUrl 지금 바로 열 수 있는 주소. 비공개 저장소면 짧게 사는 서명 주소이고,
   *                    공개 저장소면 고정 주소다. {@code FileAsset.url} 의 고정 주소는
   *                    비공개 컨테이너에서 그대로는 열리지 않으므로 쓰지 않는다.
   */
  public static FileInfo from(FileAsset file, String readableUrl) {
    return new FileInfo(
        file.getId(), readableUrl, file.getOriginalName(), file.getContentType(), file.getSize(),
        file.getUploaderId());
  }
}
