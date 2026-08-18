package com.getit.domain.setting.category.dto;

import java.util.List;

public record CategorySummary(
    Long id,
    String name,
    List<SubCategoryBrief> subCategories
) {

  public record SubCategoryBrief(Long id, String name) { }
}
