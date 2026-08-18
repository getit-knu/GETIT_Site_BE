package com.getit.domain.setting.category.dto;

import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import java.util.List;

public class CategoryTreeResult {

  public record TrackNode(
      Long id,
      String name,
      Integer order,
      List<SubCategoryNode> subCategories
  ) {

    public static TrackNode of(Track track, List<SubCategoryNode> subCategories) {
      return new TrackNode(track.getId(), track.getName(), track.getOrder(), subCategories);
    }
  }

  public record SubCategoryNode(
      Long id,
      String name,
      Integer order,
      long lectureCount
  ) {

    public static SubCategoryNode of(SubCategory subCategory, long lectureCount) {
      return new SubCategoryNode(subCategory.getId(), subCategory.getName(), subCategory.getOrder(), lectureCount);
    }
  }
}
