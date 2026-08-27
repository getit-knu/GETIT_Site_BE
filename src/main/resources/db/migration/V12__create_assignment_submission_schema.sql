CREATE TABLE assignment_submission
(
    id            bigint       NOT NULL AUTO_INCREMENT,

    comment       TEXT         DEFAULT NULL,
    link_url      varchar(512) DEFAULT NULL,
    status        varchar(20)  NOT NULL,

    assignment_id bigint       NOT NULL,
    user_id       bigint       NOT NULL,
    file_id       bigint       DEFAULT NULL,

    submitted_at  datetime(6)  NOT NULL,
    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_assignment_submission_assignment_id_user_id UNIQUE (assignment_id, user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
