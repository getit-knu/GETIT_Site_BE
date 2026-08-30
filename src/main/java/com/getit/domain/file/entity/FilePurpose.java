package com.getit.domain.file.entity;

import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 파일 용도. 허용 확장자·최대 용량과 함께 <b>어디에 저장할지</b>를 정한다.
 *
 * <p>공개 페이지에 그대로 뜨는 이미지는 공개 저장소에 둔다. 매 요청마다 서명 주소를
 * 발급하면 5분마다 URL 이 바뀌어 브라우저 캐시가 걸리지 않는다.
 *
 * <p>강의 자료와 과제 제출물은 비공개 저장소에 둔다. 수강생·제출자만 봐야 한다.
 */
@Getter
@RequiredArgsConstructor
public enum FilePurpose {

  LECTURE_MATERIAL(
      50 * 1024 * 1024L, Set.of("pdf", "zip", "pptx", "docx", "hwp", "png", "jpg"),
      StorageVisibility.PRIVATE),
  ASSIGNMENT(
      50 * 1024 * 1024L, Set.of("zip", "pdf", "png", "jpg", "ipynb", "txt"),
      StorageVisibility.PRIVATE),
  PROFILE_IMAGE(
      5 * 1024 * 1024L, Set.of("png", "jpg", "jpeg", "webp"),
      StorageVisibility.PUBLIC),
  PROJECT_THUMBNAIL(
      5 * 1024 * 1024L, Set.of("png", "jpg", "jpeg", "webp"),
      StorageVisibility.PUBLIC),
  /** 홈 화면 활동 사진 마퀴. 공개 페이지에 그대로 뜨므로 공개 저장소에 둔다. */
  ACTIVITY_PHOTO(
      5 * 1024 * 1024L, Set.of("png", "jpg", "jpeg", "webp"),
      StorageVisibility.PUBLIC);

  private final long maxSizeBytes;
  private final Set<String> allowedExtensions;
  private final StorageVisibility visibility;

  public boolean allows(String extension) { return allowedExtensions.contains(extension.toLowerCase()); }

  /**
   * 저장 키 앞에 붙는 구분자.
   *
   * <p>저장 키만 보고 어느 저장소인지 알 수 있어야 한다. {@code downloadUrl(key)} 처럼
   * 용도를 모르는 채 호출되는 자리가 있기 때문이다.
   */
  public String keyPrefix() { return visibility.getKeyPrefix(); }
}
