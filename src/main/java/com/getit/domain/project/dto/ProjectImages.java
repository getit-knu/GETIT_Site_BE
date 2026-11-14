package com.getit.domain.project.dto;

import java.util.List;

/**
 * 프로젝트에 붙은 이미지.
 *
 * <p>설명 속 이미지는 본문에 URL 이 들어 있어 공개 응답에는 싣지 않는다. 다만 어드민·부원이 다시 저장할 때
 * 연결을 유지하려면 id 목록이 필요해서 {@code descriptionImageFileIds} 로 내려 준다.
 */
public record ProjectImages(
    List<Long> galleryFileIds,
    List<String> galleryImageUrls,
    List<Long> descriptionImageFileIds
) {

  public static final ProjectImages EMPTY = new ProjectImages(List.of(), List.of(), List.of());
}
