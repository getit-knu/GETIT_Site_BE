-- 경북대 실제 단과대학 · 학과 전체 목록 시드. 이슈 #41(PR #42, V8)이 명세서 예시만 옮긴
-- placeholder였던 것을 실제 데이터로 채운다.
--
-- V8 은 이미 병합된 마이그레이션이라 손대지 않는다(4.4 규칙). 대신:
--   1) V8 이 잘못된 이름으로 넣은 두 단과대학을 실제 이름으로 고친다(UPDATE — id 를
--      유지해야 혹시 참조 중인 FK 가 깨지지 않는다. 실제로는 User.college/major 는
--      문자열 컬럼이고 Application.collegeId/majorId 는 이 마스터 데이터 연동 전이라
--      항상 null 이라 참조가 전혀 없는 걸 확인했지만, 그래도 id 보존 쪽이 안전하다):
--        경영대학 → 경상대학, IT융합대학 → IT대학
--   2) 그 아래 있던 잘못된 학과 2건(경영학과 · 경영정보학과)을 지우고 실제 학과로 채운다.
--   3) 공과대학은 이름이 이미 맞았으니 그대로 두고 학과만 채운다.
--   4) 나머지 신규 단과대학 전부와 그 학과를 추가한다.

UPDATE college SET name = '경상대학' WHERE name = '경영대학';
UPDATE college SET name = 'IT대학' WHERE name = 'IT융합대학';

DELETE FROM major WHERE college_id = (SELECT id FROM college WHERE name = '경상대학');

INSERT INTO college (name, created_at, updated_at)
VALUES ('자연과학대학', NOW(6), NOW(6)),
       ('사회과학대학', NOW(6), NOW(6)),
       ('인문대학', NOW(6), NOW(6)),
       ('사범대학', NOW(6), NOW(6)),
       ('농업생명과학대학', NOW(6), NOW(6)),
       ('생활과학대학', NOW(6), NOW(6)),
       ('예술대학', NOW(6), NOW(6)),
       ('의과대학', NOW(6), NOW(6)),
       ('치과대학', NOW(6), NOW(6)),
       ('수의과대학', NOW(6), NOW(6)),
       ('약학대학', NOW(6), NOW(6)),
       ('간호대학', NOW(6), NOW(6)),
       ('생태환경대학 (상주캠퍼스)', NOW(6), NOW(6)),
       ('과학기술대학 (상주캠퍼스)', NOW(6), NOW(6)),
       ('독립학부 / 자율전공', NOW(6), NOW(6));

INSERT INTO major (college_id, name, created_at, updated_at)
SELECT id, '전자공학부', NOW(6), NOW(6) FROM college WHERE name = 'IT대학'
UNION ALL
SELECT id, '컴퓨터학부', NOW(6), NOW(6) FROM college WHERE name = 'IT대학'
UNION ALL
SELECT id, '전기공학과', NOW(6), NOW(6) FROM college WHERE name = 'IT대학'
UNION ALL
SELECT id, '모바일공학과', NOW(6), NOW(6) FROM college WHERE name = 'IT대학'
UNION ALL
SELECT id, '인공지능학과', NOW(6), NOW(6) FROM college WHERE name = 'IT대학'
UNION ALL
SELECT id, '기계공학부', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '신소재공학부', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '건축학부', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '토목공학과', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '응용화학공학부', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '고분자공학과', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '섬유시스템공학과', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '환경공학과', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '에너지공학부', NOW(6), NOW(6) FROM college WHERE name = '공과대학'
UNION ALL
SELECT id, '수학과', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '물리학과', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '화학과', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '생명과학부', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '통계학과', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '지구시스템과학부', NOW(6), NOW(6) FROM college WHERE name = '자연과학대학'
UNION ALL
SELECT id, '경영학부', NOW(6), NOW(6) FROM college WHERE name = '경상대학'
UNION ALL
SELECT id, '경제통상학부', NOW(6), NOW(6) FROM college WHERE name = '경상대학'
UNION ALL
SELECT id, '정치외교학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '사회학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '지리학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '문헌정보학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '심리학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '사회복지학부', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '미디어커뮤니케이션학과', NOW(6), NOW(6) FROM college WHERE name = '사회과학대학'
UNION ALL
SELECT id, '국어국문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '영어영문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '사학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '철학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '불어불문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '독어독문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '중어중문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '고고인류학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '일어일문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '한문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '노어노문학과', NOW(6), NOW(6) FROM college WHERE name = '인문대학'
UNION ALL
SELECT id, '교육학과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '국어교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '영어교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '유럽어교육학부', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '역사교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '지리교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '일반사회교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '윤리교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '수학교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '물리교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '화학교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '생물교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '지구과학교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '가정교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '체육교육과', NOW(6), NOW(6) FROM college WHERE name = '사범대학'
UNION ALL
SELECT id, '응용생명과학부', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '식물의학과', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '식품공학부', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '산림과학·조경학부', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '원예과학과', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '농업토목·생물산업공학부', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '바이오섬유소재학과', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '식품자원경제학과', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '농산업학과', NOW(6), NOW(6) FROM college WHERE name = '농업생명과학대학'
UNION ALL
SELECT id, '아동학부', NOW(6), NOW(6) FROM college WHERE name = '생활과학대학'
UNION ALL
SELECT id, '의류학과', NOW(6), NOW(6) FROM college WHERE name = '생활과학대학'
UNION ALL
SELECT id, '식품영양학과', NOW(6), NOW(6) FROM college WHERE name = '생활과학대학'
UNION ALL
SELECT id, '음악학과', NOW(6), NOW(6) FROM college WHERE name = '예술대학'
UNION ALL
SELECT id, '국악학과', NOW(6), NOW(6) FROM college WHERE name = '예술대학'
UNION ALL
SELECT id, '미술학과', NOW(6), NOW(6) FROM college WHERE name = '예술대학'
UNION ALL
SELECT id, '디자인학과', NOW(6), NOW(6) FROM college WHERE name = '예술대학'
UNION ALL
SELECT id, '의예과', NOW(6), NOW(6) FROM college WHERE name = '의과대학'
UNION ALL
SELECT id, '의학과', NOW(6), NOW(6) FROM college WHERE name = '의과대학'
UNION ALL
SELECT id, '치의예과', NOW(6), NOW(6) FROM college WHERE name = '치과대학'
UNION ALL
SELECT id, '치의학과', NOW(6), NOW(6) FROM college WHERE name = '치과대학'
UNION ALL
SELECT id, '수의예과', NOW(6), NOW(6) FROM college WHERE name = '수의과대학'
UNION ALL
SELECT id, '수의학과', NOW(6), NOW(6) FROM college WHERE name = '수의과대학'
UNION ALL
SELECT id, '약학과', NOW(6), NOW(6) FROM college WHERE name = '약학대학'
UNION ALL
SELECT id, '간호학과', NOW(6), NOW(6) FROM college WHERE name = '간호대학'
UNION ALL
SELECT id, '산림생태보호학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '식물자원학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '곤충생명과학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '관광학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '체육학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '축산학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '축산생명공학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '말/특수동물학과', NOW(6), NOW(6) FROM college WHERE name = '생태환경대학 (상주캠퍼스)'
UNION ALL
SELECT id, '건설방재공학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '정밀기계공학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '자동차공학부', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '에너지신소재·화학공학부', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '소프트웨어학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '나노소재공학부', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '스마트플랜트공학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '치위생학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '섬유패션디자인학부', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '위치정보시스템학과', NOW(6), NOW(6) FROM college WHERE name = '과학기술대학 (상주캠퍼스)'
UNION ALL
SELECT id, '행정학부', NOW(6), NOW(6) FROM college WHERE name = '독립학부 / 자율전공'
UNION ALL
SELECT id, '융합학부', NOW(6), NOW(6) FROM college WHERE name = '독립학부 / 자율전공'
UNION ALL
SELECT id, '자율전공부', NOW(6), NOW(6) FROM college WHERE name = '독립학부 / 자율전공';
