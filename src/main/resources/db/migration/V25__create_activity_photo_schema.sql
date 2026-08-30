-- 홈 화면 "부원 활동 사진" 마퀴. (이슈 #146)
--
-- file_id 는 file_asset 을 가리키지만 FK 를 걸지 않는다. 도메인끼리 테이블 수준으로
-- 묶으면 한쪽을 정리할 때 다른 쪽이 막힌다. 연결·해제는 FileConnectionService 가 맡는다.
CREATE TABLE activity_photo
(
    id         bigint      NOT NULL AUTO_INCREMENT,
    photo_order int        NOT NULL,
    file_id    bigint      NOT NULL,
    is_visible bit(1)      NOT NULL,
    created_at datetime(6) NOT NULL,
    updated_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_activity_photo_order (photo_order)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
