CREATE TABLE track
(
    id             bigint      NOT NULL AUTO_INCREMENT,

    name           varchar(50) NOT NULL,
    display_order  int         NOT NULL,

    created_at     datetime(6) NOT NULL,
    updated_at     datetime(6) NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE sub_category
(
    id             bigint      NOT NULL AUTO_INCREMENT,

    name           varchar(50) NOT NULL,
    display_order  int         NOT NULL,
    track_id       bigint      NOT NULL,

    created_at     datetime(6) NOT NULL,
    updated_at     datetime(6) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_sub_category_track_id FOREIGN KEY (track_id) REFERENCES track (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
