-- 모의투자 FESTA 추첨용 전화번호 보호.
--
-- 1. phone_number 를 앱에서 AES-256-GCM 으로 암호화해 저장한다. 암호문은
--    "v1:" + base64(nonce 12B + 암호문 + 태그 16B) 라 20자에 들어가지 않는다.
--    기존 평문 행은 앱이 기동할 때 암호화한다(PlainPhoneNumberEncryptor).
-- 2. 명단 내려받기와 번호 파기를 누가 언제 했는지 남긴다. 번호는 남기지 않는다.
ALTER TABLE festa_investor
    MODIFY phone_number varchar(100) NULL;

CREATE TABLE festa_audit_log
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    action     varchar(30) NOT NULL,
    admin_id   bigint      NULL,
    item_count bigint      NOT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_festa_audit_log_created_at (created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
