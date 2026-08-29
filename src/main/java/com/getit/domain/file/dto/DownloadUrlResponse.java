package com.getit.domain.file.dto;

import com.getit.domain.file.entity.FileAsset;

/**
 * 파일 읽기 주소. (명세 4.3)
 *
 * <p>비공개 저장소라 고정 주소로는 읽을 수 없다. 요청 시점마다 짧게 사는 주소를 발급한다.
 *
 * @param downloadUrl 서명된 읽기 주소
 * @param fileName 원본 파일명. 저장 시 보여줄 이름이다
 * @param contentType 파일 형식
 * @param expiresIn 유효 시간(초)
 */
public record DownloadUrlResponse(
    String downloadUrl,
    String fileName,
    String contentType,
    int expiresIn
) {

  public static DownloadUrlResponse of(FileAsset file, String downloadUrl, int expiresIn) {
    return new DownloadUrlResponse(
        downloadUrl, file.getOriginalName(), file.getContentType(), expiresIn);
  }
}
