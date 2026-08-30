package com.getit.domain.setting.photo.dto;

import com.getit.domain.setting.photo.entity.ActivityPhoto;

/** 공개 홈 마퀴용. 노출 대상만 순서대로 내려간다. */
public record ActivityPhotoPublicResult(
    Long id,
    String imageUrl,
    int order
) {

  public static ActivityPhotoPublicResult from(ActivityPhoto photo, String imageUrl) {
    return new ActivityPhotoPublicResult(photo.getId(), imageUrl, photo.getOrder());
  }
}
