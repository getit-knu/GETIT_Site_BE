-- 프로젝트 쇼케이스 스키마. 이슈 #114 Project 엔티티에 대응한다. (명세서 D12 + 2.4)
-- 번호는 임시(V19~V22 는 열린 PR #101·#102·#107 이 선점). 머지 직전 origin/main 기준 재확인.

CREATE TABLE project
(
    id            bigint       NOT NULL AUTO_INCREMENT,

    title         varchar(100) NOT NULL,
    team_name     varchar(100) NOT NULL,
    semester      varchar(50)  NOT NULL,
    description   text         NULL,
    tech_stacks   varchar(500) NULL,
    code_url      varchar(512) NULL,
    demo_url      varchar(512) NULL,
    is_featured   bit(1)       NOT NULL,
    project_order int          NOT NULL,
    file_id       bigint       NULL,

    created_at    datetime(6)  NOT NULL,
    updated_at    datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    KEY idx_project_semester_order (semester, project_order, id),
    KEY idx_project_featured_order (is_featured, project_order, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
