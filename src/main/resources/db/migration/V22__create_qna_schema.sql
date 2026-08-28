-- Q&A 스키마. 이슈 #107 Question · Answer 엔티티에 대응한다. (명세서 D11 + 4.6/4.7)
-- 번호는 임시(V19~V21 은 열린 PR #101·#102 가 선점). 머지 직전 origin/main 기준 재확인.

CREATE TABLE question
(
    id         bigint        NOT NULL AUTO_INCREMENT,

    content    varchar(2000) NOT NULL,
    status     varchar(20)   NOT NULL,
    author_id  bigint        NOT NULL,
    lecture_id bigint        NULL,

    created_at datetime(6)   NOT NULL,
    updated_at datetime(6)   NOT NULL,

    PRIMARY KEY (id),
    KEY idx_question_lecture_id (lecture_id),
    KEY idx_question_status (status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE answer
(
    id          bigint        NOT NULL AUTO_INCREMENT,

    content     varchar(2000) NOT NULL,
    question_id bigint        NOT NULL,
    admin_id    bigint        NOT NULL,

    created_at  datetime(6)   NOT NULL,
    updated_at  datetime(6)   NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_answer_question_id (question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
