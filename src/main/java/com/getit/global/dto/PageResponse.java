package com.getit.global.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 목록 API 공통 래퍼. (API 명세서 0.3)
 * page 는 0부터 시작한다.
 */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast()
    );
  }

  /**
   * 내용 없이 페이지 메타데이터만 옮긴다. 원본이 비어 변환할 것이 없을 때 쓴다.
   *
   * <p>{@code from(page, item -> null)} 로도 같은 결과가 나오지만, 비어 있지 않은 페이지에
   * 잘못 쓰면 null 이 든 목록이 만들어진다. 의도를 이름으로 못 박는다 (PR #189 리뷰 지적).
   */
  public static <T> PageResponse<T> empty(Page<?> page) {
    return new PageResponse<>(
        List.of(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast()
    );
  }

  /** 엔티티 Page 를 응답 DTO 로 변환하면서 감쌀 때 사용한다. */
  public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return from(page.map(mapper));
  }
}
