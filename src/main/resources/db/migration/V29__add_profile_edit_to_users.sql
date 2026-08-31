-- 본인 프로필 자기 수정. (이슈 #147)
--
-- profile_file_id
--   사용자가 직접 올린 프로필 사진의 파일 id. 사진을 바꿀 때 이전 파일의 연결을 풀려고
--   들고 있는다. 화면에 쓰는 주소는 그대로 profile_image_url 에 저장한다. 프로필 사진은
--   공개 컨테이너에 두어 주소가 고정이므로, 읽을 때마다 서명을 새로 발급할 필요가 없다.
--
-- profile_customized
--   한 번이라도 스스로 고쳤는지. OAuth 재로그인마다 이름과 사진을 구글 값으로 덮어쓰기
--   때문에, 이 표시가 없으면 자기 수정한 값이 다음 로그인에 조용히 사라진다.
ALTER TABLE users
    ADD COLUMN profile_file_id    bigint     DEFAULT NULL AFTER profile_image_url,
    ADD COLUMN profile_customized bit(1)     NOT NULL DEFAULT 0 AFTER profile_file_id;
