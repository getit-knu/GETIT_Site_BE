package com.getit.domain.festa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FESTA 설정. 번호 암호화 키는 {@code getit.crypto.phone-key} 로 옮겼다(이슈 #239).
 *
 * @param phoneRetentionDays 투표 종료 뒤 번호를 보관하는 날 수. 지나면 자동 파기한다
 */
@ConfigurationProperties(prefix = "getit.festa")
public record FestaProperties(Integer phoneRetentionDays) {

  public int retentionDays() {
    return phoneRetentionDays == null ? 30 : phoneRetentionDays;
  }
}
