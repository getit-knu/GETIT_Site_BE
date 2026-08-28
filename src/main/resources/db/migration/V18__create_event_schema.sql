-- 행사 일정 스키마. 이슈 #97 Event 엔티티에 대응한다. (명세서 10.14 ~ 10.17)
-- `event` 는 MySQL 비예약어라 인용 없이도 동작하지만, Event Scheduler 키워드와 겹쳐 혼동을
-- 부르므로 식별자를 backtick 으로 감싼다. 엔티티 @Table(name = "`event`") 와 짝.

CREATE TABLE `event`
(
    id             bigint       NOT NULL AUTO_INCREMENT,

    title          varchar(100) NOT NULL,
    place          varchar(100) NOT NULL,
    start_date     date         NOT NULL,
    end_date       date         NOT NULL,
    is_visible     bit(1)       NOT NULL,
    type           varchar(20)  NOT NULL,
    generation_id  bigint       NOT NULL,

    created_at     datetime(6)  NOT NULL,
    updated_at     datetime(6)  NOT NULL,

    PRIMARY KEY (id),
    KEY idx_event_generation_id_start_date (generation_id, start_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
