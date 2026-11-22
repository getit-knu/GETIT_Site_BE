-- 모의투자 FESTA 경품 당첨자. 서버가 뽑고 당첨자 번호만 보여준다. (이슈 #238)
--
-- 번호는 복사하지 않고 festa_investor 를 user_id 로 찾아 읽는다. 번호를 파기하면 당첨자 번호도 사라진다.
-- 한 사람이 두 번 당첨되지 않게 user_id 를 유일하게 둔다.
CREATE TABLE festa_winner
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    user_id    bigint      NOT NULL,
    draw_order int         NOT NULL,
    drawn_by   bigint      NOT NULL,
    drawn_at   datetime(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_festa_winner_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
