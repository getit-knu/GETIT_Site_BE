package com.getit.domain.setting.category.dto;

import java.util.List;

/**
 * 부원용 대분류(트랙) 목록 한 줄. (이슈 #150)
 *
 * <p>강의 목록의 {@code tabs} 는 소분류 단위로 만들어져서, <b>소분류가 없는 트랙은
 * 항목 자체가 생기지 않는다.</b> 발행된 강의가 하나도 없는 트랙도 마찬가지다.
 * 그래서 화면이 트랙 구조를 알려면 별도로 받아야 한다.
 *
 * @param subCategories 소분류. 없으면 빈 배열이다 — 이 경우에도 트랙은 목록에 나온다
 */
public record TrackResult(
    Long id,
    String name,
    List<SubCategory> subCategories
) {

  public record SubCategory(Long id, String name) { }

  public static TrackResult from(CategorySummary summary) {
    return new TrackResult(
        summary.id(),
        summary.name(),
        summary.subCategories().stream()
            .map(sub -> new SubCategory(sub.id(), sub.name()))
            .toList());
  }
}
