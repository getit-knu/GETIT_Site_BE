package com.getit.domain.festa.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 경품 당첨자. 연락을 위해 전체 번호를 준다. 번호를 파기했으면 {@code phoneNumber} 가 null. (이슈 #238)
 */
public record FestaWinnersResult(List<Winner> winners) {

  public record Winner(int drawOrder, Long userId, String name, String phoneNumber, OffsetDateTime drawnAt) { }
}
