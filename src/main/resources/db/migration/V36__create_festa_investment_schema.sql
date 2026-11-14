-- 모의투자 FESTA 가상 투자 투표. (이슈 #222)
--
-- user_id · project_id 는 users · project 를 가리키지만 FK 를 걸지 않는다. 도메인끼리 테이블
-- 수준으로 묶으면 한쪽을 정리할 때 다른 쪽이 막힌다.

-- 사용자가 프로젝트마다 넣은 가상 투자금. 저장할 때 사용자의 기존 행을 통째로 바꾼다.
CREATE TABLE festa_investment
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    user_id    bigint      NOT NULL,
    project_id bigint      NOT NULL,
    amount     bigint      NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_festa_investment_user_project (user_id, project_id),
    KEY idx_festa_investment_project (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 추첨용 전화번호. 투자 내역과 나눠 두어 행사 뒤 번호만 지울 수 있게 한다.
-- 번호를 파기하면 phone_number 와 privacy_consented_at 을 비우고 행은 남긴다.
CREATE TABLE festa_investor
(
    id                   bigint       NOT NULL AUTO_INCREMENT,
    user_id              bigint       NOT NULL,
    phone_number         varchar(20)  NULL,
    privacy_consented_at datetime(6)  NULL,
    created_at           datetime(6)  NOT NULL,
    updated_at           datetime(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_festa_investor_user (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
