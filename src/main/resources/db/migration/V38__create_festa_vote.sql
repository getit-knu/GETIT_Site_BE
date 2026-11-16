-- 모의투자 FESTA 1인 1팀 투표. (이슈 #232)
--
-- 이번 학기 FESTA 는 가상 투자 대신 한 사람이 한 팀에 투표한다. 투자 테이블은 다음 학기에
-- 다시 쓸 수 있어 그대로 두고, 표는 따로 둔다. 전화번호는 festa_investor 를 같이 쓴다.
-- user_id · project_id 는 FK 를 걸지 않는다 (V36 과 같은 이유).
CREATE TABLE festa_vote
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    user_id    bigint      NOT NULL,
    project_id bigint      NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_festa_vote_user (user_id),
    KEY idx_festa_vote_project (project_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
