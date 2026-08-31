-- 부원이 낸 프로젝트의 소유 조를 id 로 못 박는다. (PR #197 코드리뷰)
--
-- "우리 조 프로젝트" 를 조 이름으로 찾고 있었는데, 조 이름은 기수 안에서만 유일하고
-- (uk_user_group_generation_name) 어드민이 바꿀 수도 있다. 그래서 이름으로 찾으면
--
--   - 같은 이름을 쓴 지난 기수 조의 프로젝트가 현재 조원에게 보이고
--   - 어드민이 팀 이름을 그렇게 적어 등록한 프로젝트까지 섞이고
--   - 조 이름을 바꾸면 그 조가 낸 것이 통째로 사라진다.
--
-- team_name 은 화면에 그대로 보여줄 표시용으로 남긴다. 소유 판정만 group_id 로 한다.
ALTER TABLE project
    ADD COLUMN group_id bigint NULL AFTER team_name;

-- 이미 있는 행을 옮긴다. 같은 이름의 조가 둘 이상이면 어느 쪽인지 정할 수 없으므로 건너뛴다
-- (그런 행은 group_id 가 NULL 로 남아 어느 조의 목록에도 나오지 않는다 — 섞어 보여주는
--  것보다 낫다). 어드민이 직접 등록한 프로젝트는 애초에 조가 없어 그대로 NULL 이다.
UPDATE project p
SET p.group_id = (SELECT g.id FROM user_group g WHERE g.name = p.team_name)
WHERE (SELECT COUNT(*) FROM user_group g WHERE g.name = p.team_name) = 1;

-- 부원 목록은 group_id 로 찾아 최신순으로 읽는다. 없으면 데이터가 쌓일수록 매 요청마다
-- project 전체를 훑는다.
CREATE INDEX idx_project_group_id ON project (group_id, id);
