-- 조 관리 스키마. 이슈 #59 에서 만든 Group 엔티티 + User.groupId 필드에 대응한다.
--
-- 주의: 이번엔 로컬 Docker 가 꺼져 있어 ddl-auto: update 로 만든 실제 DDL 을 대조하지 못했다.
-- V1~V10 컨벤션(타입 매핑 · 네이밍)을 그대로 따라 손으로 작성했다. 다음에 Docker 를 띄울 수
-- 있을 때 실제 생성 DDL 과 대조해서 차이가 없는지 한 번 확인할 것.

CREATE TABLE user_group
(
    id            bigint      NOT NULL AUTO_INCREMENT,

    generation_id bigint      NOT NULL,
    name          varchar(50) NOT NULL,

    created_at    datetime(6) NOT NULL,
    updated_at    datetime(6) NOT NULL,

    PRIMARY KEY (id),
    -- 같은 기수 안에서만 조 이름이 유일하면 된다
    UNIQUE KEY uk_user_group_generation_name (generation_id, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 소속 조. 미배정이면 NULL. 매핑 테이블 없이 FK 값만 갖는다 (한 사용자는 조 하나만 소속 가능).
ALTER TABLE users
    ADD COLUMN group_id bigint DEFAULT NULL AFTER generation_no;
