package com.getit.domain.setting.category.dto;

import java.util.List;

/** 다른 도메인에 내주는 트랙+소분류 요약. (이슈 #25, 8.1 목록의 탭 필터용) */
public record CategorySummary(
    Long id,
    String name,
    List<SubCategoryBrief> subCategories
) {

  public record SubCategoryBrief(Long id, String name) { }
}
