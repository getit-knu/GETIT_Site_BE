package com.getit.global.audit;

import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 어드민 개인정보 열람 · 내려받기를 기록한다. (이슈 #237)
 *
 * <p>호출하는 서비스의 트랜잭션에 함께 묶인다 — 기록이 실패하면 번호도 내주지 않는다. 그래서 부르는
 * 메서드는 읽기 전용이 아닌 {@code @Transactional} 이어야 한다(읽기 전용 연결에서는 INSERT 가 막힌다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuditRecorder {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final AdminAuditLogRepository repository;

  @Transactional
  public void record(AdminAuditAction action, Long adminId, Long targetId, long itemCount) {
    repository.save(AdminAuditLog.of(action, adminId, targetId, itemCount, LocalDateTime.now(SEOUL)));
    log.info("어드민 개인정보 접근. action={} adminId={} targetId={} count={}", action, adminId, targetId, itemCount);
  }
}
