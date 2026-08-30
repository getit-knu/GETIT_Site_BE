-- 복수 운영진 채점 지원. (이슈 #151)
--
-- 기존 구조는 (지원서, 기준) 당 점수가 하나뿐이라 누가 매겼는지 남지 않았다.
-- 운영진 A 가 매긴 뒤 B 가 같은 기준을 매기면 A 의 점수가 흔적 없이 사라졌다.
-- 합불을 이 점수로 정하는데 마지막에 저장한 사람의 값만 남는 상태였다.
--
-- evaluator_id 를 넣고 유니크 키를 (지원서, 기준, 평가자) 로 바꾼다.
-- 이전 구조에서 넘어온 행은 평가자를 알 수 없으므로 0 으로 둔다.
ALTER TABLE evaluation_score
    ADD COLUMN evaluator_id bigint NOT NULL DEFAULT 0 AFTER criterion_id;

ALTER TABLE evaluation_score
    ALTER COLUMN evaluator_id DROP DEFAULT;

ALTER TABLE evaluation_score
    DROP INDEX uk_evaluation_score_application_criterion;

ALTER TABLE evaluation_score
    ADD UNIQUE KEY uk_evaluation_score_app_criterion_evaluator (application_id, criterion_id, evaluator_id);

-- 지원서 하나의 모든 평가자 점수를 한 번에 읽는 조회가 주 사용처다.
CREATE INDEX idx_evaluation_score_application ON evaluation_score (application_id);
