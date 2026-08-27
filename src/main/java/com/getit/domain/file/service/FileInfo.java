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

  public static FileInfo from(FileAsset file) {
    return new FileInfo(
        file.getId(), file.getUrl(), file.getOriginalName(), file.getContentType(), file.getSize(),
        file.getUploaderId());
  }
}
