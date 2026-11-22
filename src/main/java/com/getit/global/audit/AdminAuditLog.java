package com.getit.global.audit;

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
 * 누가 언제 누구의 번호를 열람하거나 명단을 내려받았는지. 번호 자체는 남기지 않는다. (이슈 #237)
 *
 * <p>어드민 계정이 털렸을 때 무엇이 언제 나갔는지 되짚기 위해 DB 에 둔다. 수정·삭제 메서드를 두지 않는다.
 * FESTA 명단은 따로 {@code festa_audit_log} 에 남는다.
 */
@Entity
@Table(name = "admin_audit_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAuditLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private AdminAuditAction action;

  @Column(name = "admin_id", nullable = false)
  private Long adminId;

  /** 단건 조회의 대상 id. 내려받기면 null. */
  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "item_count", nullable = false)
  private long itemCount;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  static AdminAuditLog of(AdminAuditAction action, Long adminId, Long targetId, long itemCount, LocalDateTime at) {
    AdminAuditLog log = new AdminAuditLog();
    log.action = action;
    log.adminId = adminId;
    log.targetId = targetId;
    log.itemCount = itemCount;
    log.createdAt = at;
    return log;
  }
}
