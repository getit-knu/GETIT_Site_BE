-- 서류 평가 점수 스키마. 이슈 #49 에서 만든 EvaluationScore 엔티티에 대응한다.
--
-- V1 ~ V8 과 마찬가지로 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.
-- 컬럼 순서는 엔티티 필드 선언 순서로 맞추고, 제약 이름을 읽기 좋게 정리했다.
--
-- 현재 main 최신 버전이 V8(#41)이라 V9 가 다음 번호이지만, feat/#25-lecture-crud(PR #43, 아직
-- 병합 전)가 이미 V9(create_lecture_schema)를 선점하고 있어 충돌을 피하려고 V10 으로 잡았다
-- (V7 코멘트와 동일한 이유 — 실제 병합 순서에 따라 재조정이 필요할 수 있다).

CREATE TABLE evaluation_score
(
    id             bigint      NOT NULL AUTO_INCREMENT,

    application_id bigint      NOT NULL,
    criterion_id   bigint      NOT NULL,
    score          int         NOT NULL,

    created_at     datetime(6) NOT NULL,
    updated_at     datetime(6) NOT NULL,

    PRIMARY KEY (id),
    -- 지원서 하나에 평가 기준 하나당 점수는 하나다.
    UNIQUE KEY uk_evaluation_score_application_criterion (application_id, criterion_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
