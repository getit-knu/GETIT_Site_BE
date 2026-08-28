CREATE TABLE feedback
(
    id            bigint       NOT NULL AUTO_INCREMENT,

    content       TEXT         NOT NULL,

    submission_id bigint       NOT NULL,
    admin_id      bigint       NOT NULL,

    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    KEY idx_feedback_submission_id (submission_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
