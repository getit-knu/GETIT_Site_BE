package com.getit.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 공개 상태. (이슈 #148)
 *
 * <p>부원이 낸 프로젝트는 {@code PENDING} 으로 들어오고, 어드민이 승인해야 공개 쇼케이스
 * (명세서 2.4)에 나온다. 공개 홈에 바로 노출되는 자리라 사람이 한 번 보고 넘긴다.
 *
 * <p>어드민이 직접 등록한 것은 처음부터 {@code APPROVED} 다. 자기가 올린 것을 자기가
 * 다시 승인할 이유가 없다.
 */
@Getter
@RequiredArgsConstructor
public enum ProjectStatus {

  PENDING("승인 대기"),
  APPROVED("공개"),
  REJECTED("반려");

  private final String label;
}
