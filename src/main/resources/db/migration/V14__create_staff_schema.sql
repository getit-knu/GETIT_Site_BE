-- 운영진 프로필 스키마. 이슈 #81 에서 만든 Staff 엔티티에 대응한다.
--
-- 주의: 로컬 Docker 가 Windows 포트 예약 문제로 뜨지 않아 ddl-auto: update 로 만든 실제 DDL 을
-- 대조하지 못했다 (V11 · V13 과 동일한 상황). V1~V13 컨벤션(타입 매핑 · 네이밍)을 그대로 따라
-- 손으로 작성했다. 다음에 Docker 를 띄울 수 있을 때 실제 생성 DDL 과 대조해서 차이가 없는지
-- 한 번 확인할 것.

CREATE TABLE staff
(
    id             bigint       NOT NULL AUTO_INCREMENT,

    generation_no  int          NOT NULL,
    staff_order    int          NOT NULL,
    section        varchar(20)  NOT NULL,
    staff_role     varchar(50)  NOT NULL,
    name           varchar(50)  NOT NULL,
    department     varchar(100) NOT NULL,
    introduction   varchar(255) DEFAULT NULL,
    user_id        bigint       DEFAULT NULL,
    file_id        bigint       DEFAULT NULL,

    created_at     datetime(6)  NOT NULL,
    updated_at     datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    -- 10.21(목록)이 generation_no 로 필터링한 뒤 section, staff_order 로 정렬한다.
    -- 10.22(순서 변경)도 generation_no + section 으로 대상을 좁힌다.
    KEY idx_staff_generation_no_section_order (generation_no, section, staff_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
