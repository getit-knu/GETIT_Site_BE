package com.getit.domain.setting.photo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.getit.domain.setting.photo.entity.ActivityPhoto;

/** 어드민 목록·상세. 관리 화면은 fileId 도 알아야 교체할 수 있다. */
public record ActivityPhotoResult(
    Long id,
    Integer order,
    Long fileId,
    String imageUrl,
    @JsonProperty("isVisible") boolean isVisible
) {

  public static ActivityPhotoResult from(ActivityPhoto photo, String imageUrl) {
    return new ActivityPhotoResult(
        photo.getId(), photo.getOrder(), photo.getFileId(), imageUrl, photo.isVisible());
  }
}
