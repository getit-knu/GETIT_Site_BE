-- 단과대학 · 전공 마스터 데이터 스키마. 이슈 #41 에서 만든 College · Major 엔티티에 대응한다.
--
-- 로컬(local, ddl-auto: update)에서 Hibernate 가 생성한 DDL 을 옮긴 것이다.
--
-- 현재 최신 버전이 V5(#33)라 V8 로 잡았다. PR #37(V6, 평가 기준) · PR #39(V7, 지원서)가
-- 아직 병합 전이라, 실제 병합 순서에 따라 버전 재조정이 필요할 수 있다.
--
-- ⚠️ 명세서(2.6 · 2.7) 어디에도 이 데이터를 추가 · 수정 · 삭제하는 관리자 API가 없다.
-- 즉 College · Major 는 마이그레이션의 시드 데이터로만 채워진다. 아래 시드는 명세서 예시에
-- 나온 것만 옮긴 placeholder 다 — 실제 전체 단과대학 · 전공 목록은 별도로 받아서 채워야 한다.

CREATE TABLE college
(
    id         bigint       NOT NULL AUTO_INCREMENT,

    name       varchar(50)  NOT NULL,

    created_at datetime(6)  NOT NULL,
    updated_at datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_college_name (name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


CREATE TABLE major
(
    id         bigint      NOT NULL AUTO_INCREMENT,

    college_id bigint      NOT NULL,
    name       varchar(50) NOT NULL,

    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_major_college_name (college_id, name)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- 명세서 2.6 예시 그대로.
INSERT INTO college (name, created_at, updated_at)
VALUES ('경영대학', NOW(6), NOW(6)),
       ('공과대학', NOW(6), NOW(6)),
       ('IT융합대학', NOW(6), NOW(6));

-- 명세서 2.7 예시 그대로. college_id 는 auto_increment 값을 가정하지 않고 이름으로 조회해서 넣는다 —
-- 실제 전체 목록을 채우는 과정에서 college INSERT 순서가 바뀌어도 안전하도록 하기 위함이다 (PR #42 리뷰).
INSERT INTO major (college_id, name, created_at, updated_at)
SELECT id, '경영학과', NOW(6), NOW(6) FROM college WHERE name = '경영대학'
UNION ALL
SELECT id, '경영정보학과', NOW(6), NOW(6) FROM college WHERE name = '경영대학';
