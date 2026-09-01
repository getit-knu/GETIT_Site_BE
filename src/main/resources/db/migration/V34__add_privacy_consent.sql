-- 개인정보 수집·이용 동의 기록. (이슈 #203)
--
-- 지금까지 동의는 프론트 화면(JS 상태)에서만 막고 있었다. 그래서 두 가지가 문제였다.
--   1. /oauth2/authorization/google 을 직접 열면 동의 화면 자체를 건너뛸 수 있었다.
--   2. 정상 경로로 동의했더라도 "누가 언제 동의했는지" 기록이 서버에 전혀 없었다.
--      개인정보 분쟁에서 동의 획득 입증 책임은 처리자 쪽에 있으므로 기록이 필요하다.
--
-- users.privacy_consented_at
--   서비스 이용(로그인) 시 받은 동의 시각. 아직 동의하지 않았으면 null 이다.
--   FE 가 <a href> 로 구글에 바로 넘어가는 구조라 OAuth 진입을 서버가 막기 어려워, 로그인
--   자체는 막지 않고 로그인 후 POST /api/auth/consent 로 기록한다. 동의 화면은 콜백의
--   isNewUser 로 가려낸 신규 가입자 온보딩에서만 뜨므로, 이 기능 이전에 가입해 있던 회원은
--   이 값이 계속 null 이다. 그래서 이 값으로 다른 기능을 막지 않는다 — 막으면 기존 회원
--   전원이 잠긴다. 지원서 제출 동의는 아래 application 컬럼으로 따로 받는다.
--
-- application.privacy_consented_at
--   지원서 제출 시 받은 동의 시각. 로그인 동의와는 수집 항목·목적이 다른 별개의 동의라
--   따로 남긴다. 제출 전(DRAFT)에는 null 이다.
--
-- 고지 문구 버전은 지금 남기지 않는다. 문구가 바뀌어 "그때 어떤 문구에 동의했는지"를 가려야
-- 하는 시점에 privacy_consent_version 컬럼을 추가한다.
--
-- 두 컬럼 모두 조회 조건·정렬 키로 쓰지 않으므로 인덱스를 두지 않는다.
ALTER TABLE users
    ADD COLUMN privacy_consented_at datetime(6) DEFAULT NULL AFTER profile_customized;

ALTER TABLE application
    ADD COLUMN privacy_consented_at datetime(6) DEFAULT NULL AFTER submitted_at;
