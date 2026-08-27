package com.getit.domain.lecture.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 마감 판정처럼 시각에 따라 결과가 갈리는 로직을 테스트 가능하게 하기 위해
// global/config로 이동하는 편이 좋을 것 같다.
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of("Asia/Seoul"));
  }
}
