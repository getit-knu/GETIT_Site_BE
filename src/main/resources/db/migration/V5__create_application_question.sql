-- 지원서 질문 항목 스키마. 이슈 #33 에서 만든 ApplicationQuestion 엔티티에 대응한다.
--
-- V1 · V2 · V3 와 마찬가지로 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.
-- 컬럼 순서는 엔티티 필드 선언 순서로 맞추고, 제약 이름을 읽기 좋게 정리했다.
--
-- Hibernate 가 type 컬럼에 CHECK 제약(값이 TEXT/CHOICE/CHECKBOX 인지)을 자동으로 붙여줬는데,
-- 일부러 빼고 옮겼다. role · status(V1)처럼 varchar 로 고정한 이유가 "값을 추가할 때마다
-- 스키마 변경이 필요 없게" 하려는 것인데, 이 CHECK 이 있으면 네이티브 ENUM 과 똑같은 문제가 생긴다.

CREATE TABLE application_question
(
    id             bigint       NOT NULL AUTO_INCREMENT,

    generation_id  bigint       NOT NULL,

    -- order 는 SQL 예약어라 컬럼명을 분리했다.
    question_order int          NOT NULL,

    -- TEXT · CHOICE · CHECKBOX. 네이티브 ENUM 이나 CHECK 제약을 쓰지 않는다.
    type           varchar(20)  NOT NULL,
    content        varchar(500) NOT NULL,
    required       bit(1)       NOT NULL,
    -- TEXT 타입에서만 쓴다.
    max_length     int          DEFAULT NULL,
    -- CHOICE · CHECKBOX 의 선택지. [{ "id": "sw", "label": "SW 개발" }, ...] 형태로 저장한다.
    options        json         DEFAULT NULL,

    created_at     datetime(6)  NOT NULL,
    updated_at     datetime(6)  NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
