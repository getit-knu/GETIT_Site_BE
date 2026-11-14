-- 프로젝트 필드 확장. (이슈 #221)
--
-- 한 줄 소개는 FESTA 쇼케이스 카드의 소개 문구다. 지금은 description 을 2줄로 잘라 쓴다.
-- 새 필드는 모두 선택값이라 기존 프로젝트는 NULL 로 남는다.
ALTER TABLE project
    ADD COLUMN one_liner       varchar(60)  NULL,
    ADD COLUMN progress_status varchar(20)  NULL,
    ADD COLUMN play_store_url  varchar(512) NULL,
    ADD COLUMN app_store_url   varchar(512) NULL;

-- 프로젝트 이미지. 갤러리(스크린샷, 순서 유지)와 설명 본문 속 이미지를 kind 로 나눈다.
-- file_id 는 file_asset 을 가리키지만 FK 를 걸지 않는다. 연결·해제는 FileConnectionService 가 맡는다.
CREATE TABLE project_image
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    project_id bigint      NOT NULL,
    file_id    bigint      NOT NULL,
    kind       varchar(20) NOT NULL,
    sort_order int         NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_image_file (file_id),
    KEY idx_project_image_project (project_id, kind, sort_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
