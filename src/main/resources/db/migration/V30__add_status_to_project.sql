-- 부원이 낸 프로젝트의 승인 흐름. (이슈 #148)
--
-- 부원 등록분은 PENDING 으로 들어오고 어드민이 승인해야 공개 쇼케이스(2.4)에 나온다.
-- 어드민이 직접 등록한 것은 처음부터 APPROVED 다.
--
-- 기본값을 APPROVED 로 두어 이미 있는 행은 그대로 공개를 유지한다. 지금까지 등록된 것은
-- 전부 어드민이 직접 넣어 이미 공개돼 있던 자료라, PENDING 으로 내리면 화면에서 사라진다.
--
-- 네이티브 ENUM 을 쓰지 않는 것은 users.role 과 같은 이유다 — 값을 추가할 때마다
-- ALTER TABLE 이 필요해진다.
ALTER TABLE project
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'APPROVED' AFTER project_order;

CREATE INDEX idx_project_status ON project (status);
