package com.getit.domain.festa.scheduler;

import com.getit.domain.festa.service.FestaAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 투표가 끝나고 보관 기간(기본 30일)이 지나면 추첨용 번호를 자동 파기한다.
 *
 * <p>수동 파기만 있으면 잊었을 때 번호가 무기한 남는다. 일정(festa_schedule)이 없으면 언제 끝났는지
 * 모르므로 아무것도 하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FestaPhoneRetentionScheduler {

  private final FestaAdminService festaAdminService;

  @Scheduled(cron = "0 45 4 * * *", zone = "Asia/Seoul")
  public void destroyExpiredPhoneNumbers() {
    festaAdminService.destroyPhoneNumbersIfExpired();
  }
}
