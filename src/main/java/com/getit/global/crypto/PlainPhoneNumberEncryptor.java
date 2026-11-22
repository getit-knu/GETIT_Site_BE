package com.getit.global.crypto;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 기동할 때 평문 번호를 암호화하고, 키 교체 중이면 이전 키로 된 값을 새 키로 다시 암호화한다. (이슈 #239)
 *
 * <p>컨버터는 평문도 읽게 해 두었지만, 그대로 두면 DB 에 평문이 남는다. 한 번 바꾸면 다음 기동부터
 * 대상이 없어 아무 일도 하지 않는다. 엔티티를 거치지 않고 저장값을 직접 읽고 쓴다 — 컨버터가 끼면
 * 평문인지 암호문인지 구분할 수 없다. 번호는 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlainPhoneNumberEncryptor {

  /** 번호 칸이 있는 테이블. 모두 {@code id} 와 {@code phone_number} 를 쓴다. */
  static final List<String> TABLES = List.of("users", "application", "festa_investor");

  private final JdbcTemplate jdbcTemplate;
  private final PhoneNumberCipher cipher;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void encryptPlainRows() {
    for (String table : TABLES) {
      int changed = encrypt(table);
      if (changed > 0) {
        log.info("{} 전화번호 {}건을 암호화했습니다. rotating={}", table, changed, cipher.rotating());
      }
    }
  }

  private int encrypt(String table) {
    // 키 교체 중이 아니면 이미 암호화된 값은 볼 필요가 없다.
    String where = cipher.rotating()
        ? "phone_number is not null"
        : "phone_number is not null and phone_number not like 'v1:%'";
    List<Row> rows = jdbcTemplate.query(
        "select id, phone_number from " + table + " where " + where,
        (rs, i) -> new Row(rs.getLong("id"), rs.getString("phone_number")));
    int changed = 0;
    for (Row row : rows) {
      if (cipher.needsEncryption(row.stored())) {
        String plain = cipher.decrypt(row.stored());
        jdbcTemplate.update("update " + table + " set phone_number = ? where id = ?", cipher.encrypt(plain), row.id());
        changed++;
      }
    }
    return changed;
  }

  private record Row(long id, String stored) { }
}
