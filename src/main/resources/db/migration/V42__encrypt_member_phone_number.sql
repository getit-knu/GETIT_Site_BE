-- 부원 · 지원서 전화번호를 앱에서 AES-256-GCM 으로 암호화해 저장한다. (이슈 #239)
--
-- 암호문은 "v1:" + base64(nonce 12B + 암호문 + 태그 16B) 라 20자에 들어가지 않는다.
-- 기존 평문 행은 앱이 기동할 때 암호화한다(PlainPhoneNumberEncryptor).
ALTER TABLE users
    MODIFY phone_number varchar(100) NULL;

ALTER TABLE application
    MODIFY phone_number varchar(100) NULL;
