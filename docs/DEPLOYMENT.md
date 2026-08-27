# 배포 가이드 (Azure VM)

## 구조

```
인터넷 :443
   ↓
nginx (호스트)          TLS 종료 · Let's Encrypt 자동 갱신
   ↓ 127.0.0.1:8080
docker compose          app ── mysql
                         └── uploads 볼륨 (재배포해도 유지)
```

**왜 App Service 가 아니라 VM 인가**

| | VM | App Service |
|---|---|---|
| 비용 | 이미 보유. MySQL 도 같은 VM → 추가 $0 | 월 ~$25 |
| 업로드 파일 | 디스크 볼륨에 유지 | 컨테이너 휘발 → Blob 구현 선행 필요 |
| 스펙 | 2코어 7.7GB | B1 은 1코어 1.75GB |

`LocalFileStorage` 를 그대로 쓸 수 있다는 점이 결정적이었다.
App Service 였다면 `AzureBlobFileStorage` 구현이 끝날 때까지 배포를 미뤄야 했다.

## 지금 상태

| 단계 | 상태 |
|---|---|
| Dockerfile | ✅ 로컬 빌드 · 기동 검증 완료 |
| 이미지 빌드 · GHCR 푸시 | ✅ `main` 머지마다 자동 |
| VM (40.82.154.5) | ⬜ Ubuntu 24.04, 아무것도 미설치 |
| TLS · 도메인 | ⬜ |
| 자동 배포 | ⬜ `VM_DEPLOY_ENABLED` 를 켜면 동작 |

---

## 최초 세팅

### 1. 공인 IP 를 정적으로

Azure 포털 → 가상 머신 → 네트워킹 → 공인 IP → **할당: 정적**

동적이면 VM 을 중지·시작할 때 주소가 바뀌어 DNS 와 인증서가 모두 깨진다.

### 2. DNS 이름 (무료)

포털의 공인 IP → 구성 → **DNS 이름 레이블** 입력

```
getit-api  →  getit-api.koreacentral.cloudapp.azure.com
```

Let's Encrypt 인증서가 이 주소로 정상 발급된다. 도메인을 사기 전까지 이걸 쓴다.

### 3. VM 세팅

```bash
scp -i GETIT_key.pem deploy/setup-vm.sh azureuser@40.82.154.5:/tmp/
ssh -i GETIT_key.pem azureuser@40.82.154.5 'sudo bash /tmp/setup-vm.sh'
```

docker · nginx · certbot 설치, 방화벽(22/80/443), `/opt/getit` 생성, `.env` 템플릿 작성까지 한다.
여러 번 돌려도 안전하다.

### 4. 환경변수 채우기

```bash
ssh -i GETIT_key.pem azureuser@40.82.154.5
nano /opt/getit/.env
```

| 이름 | 값 |
|---|---|
| `DB_PASSWORD` · `MYSQL_ROOT_PASSWORD` | 직접 생성 |
| `JWT_SECRET` | `openssl rand -base64 48`. **로컬 값 재사용 금지** |
| `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` | Google Cloud Console |
| `CORS_ALLOWED_ORIGINS` | 프론트 주소 |
| `OAUTH2_REDIRECT_URI` | `{프론트 주소}/oauth/callback` |
| `FILE_BASE_URL` | `https://{도메인}/api/public/files` |
| `REFRESH_COOKIE_SECURE` | `true` |
| `REFRESH_COOKIE_SAME_SITE` | 아래 참조 |

### 5. TLS 인증서

```bash
sudo bash /tmp/issue-cert.sh getit-api.koreacentral.cloudapp.azure.com you@example.com
```

nginx 리버스 프록시 구성과 인증서 발급을 함께 한다. 갱신은 `certbot.timer` 가 자동 처리한다.

### 6. Google OAuth 리디렉션 URI

Google Cloud Console → 사용자 인증 정보 → 승인된 리디렉션 URI 에 추가

```
https://{도메인}/login/oauth2/code/google
```

빠뜨리면 로그인이 `redirect_uri_mismatch` 로 실패한다.

### 7. GitHub 설정

Settings → Secrets and variables → Actions

**Variables**

```
VM_DEPLOY_ENABLED = true            ← 이걸 켜는 순간 자동 배포 시작
VM_HOST           = 40.82.154.5
VM_USER           = azureuser
HEALTHCHECK_URL   = https://{도메인}/actuator/health
```

**Secrets**

```
VM_SSH_KEY = GETIT_key.pem 내용 전체 (-----BEGIN 부터 -----END 까지)
```

Settings → Environments → `production` 생성. 승인자를 지정하면 배포 전에 사람이 확인하게 할 수 있다.

---

## 이후 팀 작업 흐름

```
PR 머지 → CI 통과 → 이미지 빌드 · GHCR 푸시 → VM 배포 → 헬스체크
```

**팀원이 할 일은 PR 머지뿐이다.** 서버에 접속할 필요가 없다.

배포는 헬스체크가 통과해야 성공으로 기록된다.
실패하면 워크플로가 빨간불이 되고 앱 로그 100줄이 Actions 로그에 남는다.

---

## 🔴 SameSite 주의

Refresh Token 쿠키가 여기 걸린다.

| 프론트 ↔ 백엔드 | 같은 사이트? | `REFRESH_COOKIE_SAME_SITE` |
|---|---|---|
| `getit.co.kr` ↔ `api.getit.co.kr` | ✅ | `Lax` (권장) |
| `getit.vercel.app` ↔ `*.cloudapp.azure.com` | ❌ | `None` |
| `localhost:5173` ↔ `*.cloudapp.azure.com` | ❌ | `None` |

**`Lax` 인데 교차 사이트면 재발급 요청에 쿠키가 실리지 않아 로그인이 유지되지 않는다.**
증상이 "로그인은 되는데 새로고침하면 풀린다" 로 나타나서 원인을 찾기 어렵다.

`None` 은 `Secure=true` 가 함께여야 브라우저가 받는다. HTTPS 를 붙였으므로 문제없다.

---

## 도메인을 산 뒤

**하나만 사면 된다.** 서브도메인은 무제한이고 추가 비용이 없다.

```
getit.co.kr        →  프론트엔드
api.getit.co.kr    →  백엔드 (이 VM)
```

같은 등록 도메인이라 `SameSite=Lax` 를 쓸 수 있다. 보안 설정이 단순해진다.

**전환 절차**

```bash
# 1. 가비아 DNS 에 A 레코드 추가
#    api.getit.co.kr  →  40.82.154.5

# 2. 인증서 재발급 (nginx 구성도 함께 갱신된다)
sudo bash issue-cert.sh api.getit.co.kr you@getit.co.kr

# 3. .env 수정
#    CORS_ALLOWED_ORIGINS     = https://getit.co.kr
#    OAUTH2_REDIRECT_URI      = https://getit.co.kr/oauth/callback
#    FILE_BASE_URL            = https://api.getit.co.kr/api/public/files
#    REFRESH_COOKIE_SAME_SITE = Lax
cd /opt/getit && docker compose up -d

# 4. Google Console 리디렉션 URI 추가
#    https://api.getit.co.kr/login/oauth2/code/google

# 5. GitHub Variables 의 HEALTHCHECK_URL 갱신
```

CD 워크플로와 compose 파일은 손대지 않는다.

---

## 운영

**로그**

```bash
ssh -i GETIT_key.pem azureuser@40.82.154.5
cd /opt/getit
docker compose logs app -f --tail 100
docker compose ps
```

**롤백** — 이미지에 커밋 SHA 태그가 붙어 있다.

Actions → CD → Run workflow → `image_tag` 에 입력

```
ghcr.io/getit-knu/getit_site_be:sha-{되돌릴 커밋}
```

⚠️ **마이그레이션은 되돌아가지 않는다.** `V{n}` 이 적용된 뒤 이전 이미지로 롤백하면
`ddl-auto: validate` 가 스키마 불일치로 기동을 막을 수 있다.
스키마를 바꾸는 배포는 롤백이 어렵다는 점을 감안해 나눠 배포한다.

**DB 백업** — 아직 자동화하지 않았다. 실사용 전에 필요하다.

```bash
docker compose exec mysql mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" getit > backup.sql
```

**디스크** — 29GB 다. CD 가 배포마다 7일 지난 이미지를 정리하고,
컨테이너 로그는 10MB × 3개로 제한했다.

---

## 아직 남은 것

| 항목 | 담당 | 비고 |
|---|---|---|
| DB 자동 백업 | B | cron + mysqldump, 또는 Azure Files 로 전송 |
| Redis | — | **만들지 않는다.** 코드에서 쓰지 않는다 |
| 모니터링 | B | 헬스체크 외에 알림 없음 |
| 프론트 배포 | R | 별도 저장소 |
