-- 평가 기준과 점수의 관계를 DB 가 보장하게 한다. (이슈 #157, PR #161 리뷰)
--
-- 애플리케이션에서 기준을 지울 때 점수를 함께 지우도록 고쳤지만, 그것만으로는
-- 경합이 남는다. 점수를 지운 직후 다른 트랜잭션이 같은 기준에 점수를 새로 저장하면
-- 기준만 사라지고 그 점수가 고아로 남는다.
--
-- FK + ON DELETE CASCADE 를 걸면 끼어드는 순서와 무관하게 불변식이 지켜진다.
-- 같은 도메인 안이라 FK 를 건다 (sub_category → track 과 같은 컨벤션).
-- 도메인이 다른 참조(file_id 등)에는 여전히 걸지 않는다.

-- 이미 고아가 된 행이 있으면 FK 를 만들 수 없다. 먼저 정리한다.
DELETE es
FROM evaluation_score es
         LEFT JOIN evaluation_criterion ec ON ec.id = es.criterion_id
WHERE ec.id IS NULL;

-- 유니크 키가 (application_id, criterion_id, evaluator_id) 순서라 criterion_id 단독
-- 조회에는 쓸 수 없다. 기준 하나를 지울 때마다 점수 전체를 훑게 된다.
CREATE INDEX idx_evaluation_score_criterion ON evaluation_score (criterion_id);

ALTER TABLE evaluation_score
    ADD CONSTRAINT fk_evaluation_score_criterion
        FOREIGN KEY (criterion_id) REFERENCES evaluation_criterion (id)
            ON DELETE CASCADE;
