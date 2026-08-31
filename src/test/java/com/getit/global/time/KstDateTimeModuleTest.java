package com.getit.global.time;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** LocalDateTime 을 한국 시간 오프셋으로 주고받는다. (이슈 #177) */
@SpringBootTest
class KstDateTimeModuleTest {

  private record Holder(LocalDateTime at) { }

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("내보낼 때 +09:00 이 붙는다")
  void writesKstOffset() throws Exception {
    String json = objectMapper.writeValueAsString(
        new Holder(LocalDateTime.of(2026, 9, 1, 14, 59)));

    // 오프셋이 없으면 브라우저가 UTC 로 읽어 9 시간을 민다 — 일정이 06:59 로 보였다.
    assertThat(json).contains("2026-09-01T14:59:00+09:00");
  }

  @Test
  @DisplayName("오프셋 없이 들어오면 한국 시간 벽시계로 읽는다")
  void readsBareTextAsKstWallClock() throws Exception {
    Holder holder = objectMapper.readValue("{\"at\":\"2026-09-15T23:59:00\"}", Holder.class);

    assertThat(holder.at()).isEqualTo(LocalDateTime.of(2026, 9, 15, 23, 59));
  }

  @Test
  @DisplayName("+09:00 이 붙어 들어오면 그대로 같은 시각이다")
  void readsKstOffset() throws Exception {
    Holder holder = objectMapper.readValue("{\"at\":\"2026-09-15T23:59:00+09:00\"}", Holder.class);

    assertThat(holder.at()).isEqualTo(LocalDateTime.of(2026, 9, 15, 23, 59));
  }

  @Test
  @DisplayName("UTC 로 들어오면 한국 시간으로 환산한다")
  void convertsUtcToKst() throws Exception {
    Holder holder = objectMapper.readValue("{\"at\":\"2026-09-15T14:59:00Z\"}", Holder.class);

    // 프론트가 UTC 로 변환해 보내던 값이다. 그대로 저장하면 마감이 9 시간 당겨진다.
    assertThat(holder.at()).isEqualTo(LocalDateTime.of(2026, 9, 15, 23, 59));
  }

  @Test
  @DisplayName("내보낸 값을 그대로 다시 읽으면 같은 시각이다")
  void roundTrips() throws Exception {
    LocalDateTime original = LocalDateTime.of(2026, 9, 6, 9, 0);

    Holder restored = objectMapper.readValue(
        objectMapper.writeValueAsString(new Holder(original)), Holder.class);

    assertThat(restored.at()).isEqualTo(original);
  }
}
