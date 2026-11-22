-- 어드민이 부원 · 지원자 전체 번호를 열람하거나 부원 엑셀을 내려받은 기록. 번호는 남기지 않는다. (이슈 #237)
CREATE TABLE admin_audit_log
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    action     varchar(40) NOT NULL,
    admin_id   bigint      NOT NULL,
    target_id  bigint      NULL,
    item_count bigint      NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_admin_audit_log_created_at (created_at),
    KEY idx_admin_audit_log_admin_id (admin_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
