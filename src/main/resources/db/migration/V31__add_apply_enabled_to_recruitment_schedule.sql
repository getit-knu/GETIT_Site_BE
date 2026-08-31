-- 모집 지원 활성화 · 비활성화 토글. (이슈 #170)
--
-- 지금까지 지원 가능 여부는 일정에서만 파생됐다. documentStartAt 이 지나면 자동으로 열리고
-- documentEndAt 까지 자동으로 열려 있어, 여는 동안 운영진이 끌 수단이 없었다.
-- 급히 멈추려면 documentEndAt 을 과거로 당기는 수밖에 없는데, 그러면 원래 마감 일정이
-- 지워지고 공개 화면의 D-day 와 일정 표시까지 함께 망가진다.
--
-- 기본값은 1(열림)이다. 이미 있는 행은 지금까지처럼 일정만으로 열리고 닫힌다.
ALTER TABLE recruitment_schedule
    ADD COLUMN apply_enabled bit(1) NOT NULL DEFAULT 1 AFTER interview_end_at;
