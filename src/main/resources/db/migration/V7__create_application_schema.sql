-- 지원서 스키마. 이슈 #38 에서 만든 Application · ApplicationAnswer 엔티티에 대응한다.
--
-- V1 ~ V5 와 마찬가지로 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.
-- 컬럼 순서는 엔티티 필드 선언 순서로 맞추고, 제약 이름을 읽기 좋게 정리했다.
--
-- Hibernate 가 application.status 컬럼에 CHECK 제약을 자동으로 붙여줬는데, application_question.type
-- 과 같은 이유로 일부러 빼고 옮겼다 — 값을 추가할 때마다 스키마 변경이 필요 없게 하기 위함이다.
--
-- 현재 최신 버전이 V5(#33)라 V7 로 잡았다. PR #37(V6, 평가 기준)이 아직 병합 전이고
-- feat/#24-Track-SubCategory-CRUD(V4)도 병합 전이라, 실제 병합 순서에 따라 버전 재조정이
-- 필요할 수 있다.

CREATE TABLE application
(
    id            bigint       NOT NULL AUTO_INCREMENT,

    user_id       bigint       NOT NULL,
    generation_id bigint       NOT NULL,

    -- DRAFT · SUBMITTED · DOC_PASS · DOC_FAIL · FINAL_PASS · FINAL_FAIL. 네이티브 ENUM 을 쓰지 않는다.
    status        varchar(20)  NOT NULL,

    -- 지원서 제출 시점 값을 그대로 담는다. User 의 값과 다를 수 있다.
    name          varchar(50)  NOT NULL,
    email         varchar(255) NOT NULL,
    phone_number  varchar(20)  DEFAULT NULL,
    -- College · Major 마스터 데이터가 아직 없어 당분간 항상 NULL 이다.
    college_id    bigint       DEFAULT NULL,
    major_id      bigint       DEFAULT NULL,
    grade         int          DEFAULT NULL,

    -- 제출 전(DRAFT)에는 NULL 이다.
    submitted_at  datetime(6)  DEFAULT NULL,

    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    -- 사용자당 기수별 지원서는 최대 1건이다.
    UNIQUE KEY uk_application_user_generation (user_id, generation_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE application_answer
(
    id               bigint      NOT NULL AUTO_INCREMENT,

    application_id   bigint      NOT NULL,
    question_id      bigint      NOT NULL,

    -- TEXT 질문의 답변.
    answer_text      text        DEFAULT NULL,
    -- CHOICE · CHECKBOX 질문이 고른 옵션 id 목록. [ "sw", "startup" ] 형태로 저장한다.
    selected_options json        DEFAULT NULL,

    created_at       datetime(6) NOT NULL,
    updated_at       datetime(6) NOT NULL,

    PRIMARY KEY (id),
    -- 질문 하나에 답변은 하나다.
    UNIQUE KEY uk_application_answer_question (application_id, question_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
