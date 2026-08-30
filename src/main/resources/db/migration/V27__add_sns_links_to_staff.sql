-- 운영진 카드의 GitHub · Instagram 링크. (이슈 #155)
--
-- 공개 소개 화면에 아이콘은 이미 있는데 연결할 주소가 없어 장식으로만 떠 있었다.
-- 계정이 없는 운영진도 있으므로 둘 다 선택값이다.
--
-- 길이는 프로젝트의 codeUrl · demoUrl 과 맞춘다.
ALTER TABLE staff
    ADD COLUMN github_url    varchar(512) DEFAULT NULL AFTER introduction,
    ADD COLUMN instagram_url varchar(512) DEFAULT NULL AFTER github_url;
