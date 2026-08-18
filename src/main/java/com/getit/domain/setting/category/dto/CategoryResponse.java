package com.getit.domain.setting.category.dto;

import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;

public class CategoryResponse {

  public record TrackResult(Long id, String name, Integer order, long lectureCount) {

    public static TrackResult of(Track track, long lectureCount) {
      return new TrackResult(track.getId(), track.getName(), track.getOrder(), lectureCount);
    }
  }

  public record SubCategoryResult(Long id, String name, Integer order, Long trackId, long lectureCount) {

    public static SubCategoryResult of(SubCategory subCategory, long lectureCount) {
      return new SubCategoryResult(
          subCategory.getId(), subCategory.getName(), subCategory.getOrder(),
          subCategory.getTrackId(), lectureCount);
    }
  }
}
