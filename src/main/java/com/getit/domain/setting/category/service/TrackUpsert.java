package com.getit.domain.setting.category.service;

import java.util.List;

public record TrackUpsert(
    Long id,
    String name,
    List<SubCategoryNode> subCategories
) {

  public record SubCategoryNode(
      Long id,
      String name
  ) { }
}
