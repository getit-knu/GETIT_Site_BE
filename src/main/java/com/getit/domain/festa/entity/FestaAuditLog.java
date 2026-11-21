package com.getit.domain.festa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 누가 언제 전화번호 명단을 내려받거나 지웠는지. 번호 자체는 남기지 않는다.
 *
 * <p>앱 로그는 서버를 다시 띄우거나 로그가 돌면 사라진다. 어드민 계정이 털렸을 때 언제부터
 * 새었는지 되짚을 수 있게 DB 에 둔다. 수정·삭제 메서드를 두지 않는다.
 */
@Entity
@Table(name = "festa_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FestaAuditAction action;

  /** 스케줄러가 한 작업이면 null. */
  @Column(name = "admin_id")
  private Long adminId;

  @Column(name = "item_count", nullable = false)
  private long itemCount;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  public static FestaAuditLog of(FestaAuditAction action, Long adminId, long itemCount, LocalDateTime at) {
    FestaAuditLog log = new FestaAuditLog();
    log.action = action;
    log.adminId = adminId;
    log.itemCount = itemCount;
    log.createdAt = at;
    return log;
  }
}
