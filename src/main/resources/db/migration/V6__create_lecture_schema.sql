-- 강의·강의자료·과제 스키마. 이슈 #25 에서 만든 Lecture/LectureFile/Assignment 엔티티에 대응한다.
--
-- V1~V5 와 마찬가지로 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.

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
    -- track_id 는 sub_category_id 로부터 유도 가능하지만, 목록 조회 트랙 필터에서
    -- category 도메인을 매번 조회하지 않도록 비정규화해 저장한다.
    track_id          bigint       NOT NULL,
    -- 소분류 없이도 강의를 만들 수 있어 nullable. force 삭제 시 이 값만 비운다(트랙 연결은 유지).
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
    id          bigint       NOT NULL AUTO_INCREMENT,

    lecture_id  bigint       NOT NULL,
    title       varchar(255) NOT NULL,
    description TEXT         DEFAULT NULL,
    deadline    datetime(6)  NOT NULL,

    created_at  datetime(6)  NOT NULL,
    updated_at  datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT uk_assignment_lecture_id UNIQUE (lecture_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
