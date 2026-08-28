package com.getit.domain.recruitment.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 모집 상태(2.8)의 D-day 계산에 쓰는 시각 소스. {@code RecruitmentStatusService} 가
 * {@code LocalDateTime.now()} 를 직접 부르면 테스트에서 단계 경계값을 고정할 수 없고, 서비스가
 * 계산한 now 와 테스트가 별도로 만든 now 사이에 자정이 끼면 D-day 검증이 간헐적으로 실패할 수
 * 있다(PR #86 Copilot 리뷰 지적). {@code Clock} 빈을 주입받게 하고 테스트에서는 고정된 Clock
 * 으로 교체한다.
 */
@Configuration
public class RecruitmentClockConfig {

  @Bean
  public Clock recruitmentClock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }
}
