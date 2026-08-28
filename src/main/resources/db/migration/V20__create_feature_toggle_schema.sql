-- 기능 활성화 토글 스키마. 이슈 #99 FeatureToggle 엔티티에 대응한다. (명세서 10.23 · 10.24)

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
