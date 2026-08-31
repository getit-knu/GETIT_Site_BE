package com.getit.global.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.springframework.boot.jackson.JsonComponent;

/**
 * {@code LocalDateTime} 을 한국 시간 오프셋(+09:00)이 붙은 값으로 주고받는다. (이슈 #177)
 *
 * <p>오프셋 없이 {@code 2026-09-01T14:59:00} 으로 내보내면 브라우저가 이걸 UTC 로 읽어
 * 9 시간을 민다. 실제로 모집 일정이 입력칸에 {@code 2026-09-02T06:59} 로 보였고, 과제 마감은
 * 14:59 UTC 로 저장됐다. 저장까지 밀리므로 일정 자체가 어긋난다.
 *
 * <p>명세서 0.4 의 DateTime 규약이 요구하는 모양이기도 하다. 일부 DTO 는 이미
 * {@code OffsetDateTime} 으로 바꿔 내보내고 있었는데({@code RecruitmentStatusResult} 등),
 * 남은 것들이 규칙에서 빠져 있었다. DTO 를 하나씩 고치는 대신 직렬화 규칙 자체를 바꿔서
 * 앞으로 만들어지는 것까지 자동으로 맞게 한다.
 *
 * <p>이 코드베이스의 {@code LocalDateTime} 은 전부 한국 시간 벽시계다 — 컨테이너의
 * {@code TZ=Asia/Seoul} 과 {@code spring.jackson.time-zone} 이 그렇게 맞춰져 있다.
 *
 * <p>받을 때는 오프셋이 붙어 오면 한국 시간으로 환산하고, 없으면 지금처럼 한국 시간
 * 벽시계로 읽는다. 프론트가 어느 쪽으로 보내도 같은 시각으로 저장된다.
 */
@JsonComponent
public class KstDateTimeModule extends SimpleModule {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  public KstDateTimeModule() {
    addSerializer(LocalDateTime.class, new KstSerializer());
    addDeserializer(LocalDateTime.class, new KstDeserializer());
  }

  private static final class KstSerializer extends JsonSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
        throws IOException {
      gen.writeString(value.atZone(SEOUL).toOffsetDateTime()
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }
  }

  private static final class KstDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context)
        throws IOException {
      String text = parser.getText();
      if (text == null || text.isBlank()) {
        return null;
      }
      try {
        // "2026-09-15T23:59:00+09:00" · "2026-09-15T14:59:00Z" 처럼 오프셋이 붙어 온 경우
        return OffsetDateTime.parse(text).atZoneSameInstant(SEOUL).toLocalDateTime();
      } catch (DateTimeParseException notAnOffset) {
        // "2026-09-15T23:59:00" — 오프셋이 없으면 한국 시간 벽시계로 읽는다
        return LocalDateTime.parse(text);
      }
    }
  }
}
