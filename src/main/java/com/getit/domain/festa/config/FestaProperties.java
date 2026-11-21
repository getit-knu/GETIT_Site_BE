package com.getit.domain.festa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FESTA 설정. 키는 커밋하지 않고 .env 의 FESTA_PHONE_KEY 에서 주입받는다.
 *
 * @param phoneKey 추첨용 전화번호 암호화 키. base64 로 인코딩한 32바이트 (openssl rand -base64 32)
 * @param phoneRetentionDays 투표 종료 뒤 번호를 보관하는 날 수. 지나면 자동 파기한다
 */
@ConfigurationProperties(prefix = "getit.festa")
public record FestaProperties(String phoneKey, Integer phoneRetentionDays) {

  public int retentionDays() {
    return phoneRetentionDays == null ? 30 : phoneRetentionDays;
  }
}
