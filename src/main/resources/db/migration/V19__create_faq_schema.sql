-- FAQ 스키마. 이슈 #98 Faq 엔티티에 대응한다. (명세서 10.18 ~ 10.19)

CREATE TABLE faq
(
    id         bigint        NOT NULL AUTO_INCREMENT,

    faq_order  int           NOT NULL,
    question   varchar(255)  NOT NULL,
    answer     varchar(2000) NOT NULL,
    is_visible bit(1)        NOT NULL,

    created_at datetime(6)   NOT NULL,
    updated_at datetime(6)   NOT NULL,

    PRIMARY KEY (id),
    -- 10.18(목록)이 faq_order 로 정렬한다.
    KEY idx_faq_order (faq_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
