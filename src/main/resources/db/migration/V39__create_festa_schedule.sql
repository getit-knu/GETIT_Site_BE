-- 모의투자 FESTA 투표 기간. (이슈 #234)
--
-- 토글(feature_toggle)은 쇼케이스 노출 스위치로 두고, 투표 가능 여부만 이 기간으로 한 번 더
-- 거른다. 행은 하나뿐이다(id = 1). 행이 없으면 미설정이고, 미설정이면 토글만 본다.
-- 시각은 Asia/Seoul 기준이다.
CREATE TABLE festa_schedule
(
    id         bigint      NOT NULL,
    start_at   datetime(6) NOT NULL,
    end_at     datetime(6) NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
