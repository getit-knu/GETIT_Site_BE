-- 기능 토글 초기 행. 키 집합은 FeatureKey enum 으로 고정이라 시드하고, 10.24 는 enabled 만 갱신한다.

INSERT INTO feature_toggle (toggle_key, enabled, updated_by, created_at, updated_at)
VALUES ('STOCK_GAME', 0, NULL, NOW(6), NOW(6)),
       ('MOCK_INVESTMENT', 0, NULL, NOW(6), NOW(6));
