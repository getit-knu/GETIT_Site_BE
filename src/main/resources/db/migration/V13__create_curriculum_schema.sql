-- 커리큘럼 스키마. 이슈 #77 에서 만든 Curriculum 엔티티에 대응한다.
--
-- 주의: 로컬 Docker 가 Windows 포트 예약 문제로 뜨지 않아 ddl-auto: update 로 만든 실제 DDL 을
-- 대조하지 못했다. V1~V12 컨벤션(타입 매핑 · 네이밍)을 그대로 따라 손으로 작성했다. 다음에
-- Docker 를 띄울 수 있을 때 실제 생성 DDL 과 대조해서 차이가 없는지 한 번 확인할 것.

CREATE TABLE curriculum
(
    id               bigint       NOT NULL AUTO_INCREMENT,

    generation_id    bigint       NOT NULL,
    curriculum_order int          NOT NULL,
    title            varchar(100) NOT NULL,
    subtitle         varchar(255) NOT NULL,

    created_at       datetime(6)  NOT NULL,
    updated_at       datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    -- 10.10(목록)이 generation_id로 필터링한 뒤 curriculum_order로 정렬한다.
    KEY idx_curriculum_generation_id_order (generation_id, curriculum_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
