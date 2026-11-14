-- 모의투자 FESTA 출전 프로젝트를 어드민이 직접 고르고 순서를 정한다. (이슈 #220)
--
-- 홈 소개용 is_featured 와는 별도 플래그다. 홈에 소개할 프로젝트와 행사에 출전하는
-- 프로젝트는 다를 수 있다.
--
-- festa_order 는 NULL 을 허용한다. 출전으로만 켜 두고 순서를 안 정한 것은 목록 맨 뒤로 간다.
ALTER TABLE project
    ADD COLUMN festa_entry boolean NOT NULL DEFAULT FALSE,
    ADD COLUMN festa_order int NULL;
