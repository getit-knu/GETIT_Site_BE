-- 기능 활성화 토글 스키마. 이슈 #99 FeatureToggle 엔티티에 대응한다. (명세서 10.23 · 10.24)
-- 키 집합은 고정(FeatureKey enum)이라 행을 시드하고, 10.24 는 enabled 만 갱신한다.

CREATE TABLE feature_toggle
(
    toggle_key   varchar(30)  NOT NULL,

    enabled      bit(1)       NOT NULL,
    updated_by   bigint       DEFAULT NULL,

    created_at   datetime(6)  NOT NULL,
    updated_at   datetime(6)  NOT NULL,

    PRIMARY KEY (toggle_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

INSERT INTO feature_toggle (toggle_key, enabled, updated_by, created_at, updated_at)
VALUES ('STOCK_GAME', 0, NULL, NOW(6), NOW(6)),
       ('MOCK_INVESTMENT', 0, NULL, NOW(6), NOW(6));
