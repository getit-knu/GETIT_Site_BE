CREATE TABLE lecture
(
    id                bigint       NOT NULL AUTO_INCREMENT,

    week              int          NOT NULL,
    title             varchar(255) NOT NULL,
    description       TEXT         DEFAULT NULL,
    youtube_url       varchar(512) DEFAULT NULL,
    material_url      varchar(512) DEFAULT NULL,
    duration_minutes  int          DEFAULT NULL,
    is_published      bit(1)       NOT NULL,

    generation_id     bigint       NOT NULL,
    track_id          bigint       DEFAULT NULL,
    sub_category_id   bigint       DEFAULT NULL,
    created_by        bigint       NOT NULL,

    created_at        datetime(6)  NOT NULL,
    updated_at        datetime(6)  NOT NULL,
    deleted_at        datetime(6)  DEFAULT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE lecture_file
(
    id           bigint       NOT NULL AUTO_INCREMENT,

    display_name varchar(255) NOT NULL,
    lecture_id   bigint       NOT NULL,
    file_id      bigint       NOT NULL,

    created_at   datetime(6)  NOT NULL,
    updated_at   datetime(6)  NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE assignment
(
    id               bigint       NOT NULL AUTO_INCREMENT,

    lecture_id       bigint       NOT NULL,
    title            varchar(255) NOT NULL,
    description      TEXT         DEFAULT NULL,
    deadline         datetime(6)  NOT NULL,
    allowed_types    varchar(20)  NOT NULL,
    link_placeholder varchar(255) DEFAULT NULL,

    created_at       datetime(6)  NOT NULL,
    updated_at       datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_assignment_lecture_id UNIQUE (lecture_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
