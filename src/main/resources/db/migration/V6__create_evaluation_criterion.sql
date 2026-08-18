-- 서류 평가 기준 스키마. 이슈 #36 에서 만든 EvaluationCriterion 엔티티에 대응한다.
--
-- V1 ~ V5 와 마찬가지로 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.
-- 컬럼 순서는 엔티티 필드 선언 순서로 맞추고, 제약 이름을 읽기 좋게 정리했다.

CREATE TABLE evaluation_criterion
(
    id              bigint       NOT NULL AUTO_INCREMENT,

    generation_id   bigint       NOT NULL,

    -- order 는 SQL 예약어라 컬럼명을 분리했다.
    criterion_order int          NOT NULL,
    name            varchar(100) NOT NULL,
    guideline       varchar(500) NOT NULL,
    max_score       int          NOT NULL,

    created_at      datetime(6)  NOT NULL,
    updated_at      datetime(6)  NOT NULL,

    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
