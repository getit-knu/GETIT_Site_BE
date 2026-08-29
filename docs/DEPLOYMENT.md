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
| Dockerfile | ✅ 빌드 · 기동 검증 완료 |
| 이미지 빌드 · GHCR 푸시 | ✅ CI 통과 시 자동 |
| VM (40.82.154.5) | ✅ docker · nginx · certbot · 방화벽 |
| 도메인 · TLS | ✅ `api.getit.io.kr`, Let's Encrypt 자동 갱신 |
| DB 백업 | ✅ 매일 04:00 (KST) |
| **자동 배포** | ✅ **동작 중.** main 머지 → CI 통과 → 배포 → 헬스체크 |
| 롤백 | ✅ 리허설 완료 (추가 전용 마이그레이션 기준) |
| 모니터링 | ⬜ 헬스체크 외 알림 없음 |
| 백업 오프디스크 복사 | ⚠️ 스크립트는 있다. Azure 스토리지 설정 전까지는 로컬에만 있다 |

**팀원이 할 일은 PR 머지뿐이다.** 서버에 접속할 필요가 없다.
CI 가 실패하면 배포는 시작조차 하지 않는다.

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
# 두 스크립트를 함께 보낸다. issue-cert.sh 는 5번 단계에서 쓴다.
scp -i GETIT_key.pem deploy/setup-vm.sh deploy/issue-cert.sh azureuser@40.82.154.5:/tmp/
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
| `FILE_BASE_URL` | `https://{도메인}/api/public/files` (로컬 저장 시에만 쓰인다) |
| `FILE_AZURE_ENABLED` | `true` 면 프론트가 Azure Blob 으로 직접 업로드 |
| `AZURE_UPLOAD_ACCOUNT` · `AZURE_UPLOAD_CONTAINER` | `getituploads01` · `uploads` |
| `AZURE_PUBLIC_ACCOUNT` · `AZURE_PUBLIC_CONTAINER` | `getitpublic01` · `public-assets` |
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
(`https://github.com/getit-knu/GETIT_Site_BE/settings/variables/actions`)

**Variables** — `New repository variable` 을 눌러 **하나씩 5개**를 만든다.

| Name | Value |
|---|---|
| `VM_DEPLOY_ENABLED` | `true` |
| `VM_HOST` | `40.82.154.5` |
| `VM_USER` | `azureuser` |
| `HEALTHCHECK_URL` | `https://api.getit.io.kr/actuator/health` |
| `VM_SSH_HOST_KEY` | `40.82.154.5 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIG6yBTIvd8cPBrFfogc9eDbk7UhtZK3h2OyqlUQO5FM4` |

🔴 **한 항목에 여러 줄을 몰아 넣으면 안 된다.** Name 칸에는 변수 이름만,
Value 칸에는 값만 들어간다. 위 목록을 통째로 붙여넣어 변수 하나로 만들면
`vars.VM_DEPLOY_ENABLED` 는 존재하지 않게 되고, 배포가 조용히 건너뛰어진다.

🔴 **Repository variables** 영역이어야 한다. 같은 화면 아래쪽의 Environment
variables 에 넣으면 job 조건에서 읽히지 않는다. 옆의 Secrets 탭도 아니다.

오타도 조심한다. `ture` 는 `true` 가 아니고 `azuresuser` 는 `azureuser` 가 아니다.
값이 무엇으로 읽혔는지는 CD 실행의 `배포 설정 확인` 스텝 로그에 그대로 찍힌다.

`VM_SSH_HOST_KEY` 는 접속할 서버가 정말 우리 VM 인지 확인하는 데 쓴다.
없으면 CD 가 접속 직전에 받은 키를 그대로 믿는데, 중간자가 끼면
compose 파일과 GHCR 토큰을 공격자 서버에 넘겨주게 된다.
비워두면 경고를 남기고 동작은 한다. VM 을 새로 만들면 아래로 다시 받는다.

```bash
ssh-keyscan -t ed25519 40.82.154.5
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

CD 는 `push` 가 아니라 CI 의 `workflow_run` 으로 시작한다.
`push` 로 받으면 CI 와 나란히 돌아 결과를 기다리지 않기 때문에,
테스트가 깨진 커밋도 운영에 올라갈 수 있었다. 지금은 CI 가 빨간불이면 배포가 시작되지 않는다.

배포는 헬스체크가 통과해야 성공으로 기록된다.
실패하면 워크플로가 빨간불이 되고 앱 로그 100줄이 Actions 로그에 남는다.

### 배포가 안 될 때

먼저 CD 실행의 **`배포 설정 확인`** 스텝 로그를 본다. 읽힌 변수 값이 그대로 찍힌다.

| 증상 | 원인 |
|---|---|
| 값이 전부 비어 있다 | 변수를 한 항목에 몰아 넣었거나 Repository variables 가 아닌 곳에 등록했다 |
| `VM_DEPLOY_ENABLED` 만 다르다 | 오타 (`ture` 등) |
| SSH 에서 `Permission denied` | `VM_USER` 오타, 또는 `VM_SSH_KEY` 가 pem 전체가 아니다 |
| `unauthorized` 로 pull 실패 | GHCR 로그인 실패. 이미지 job 은 성공했는지 확인한다 |
| 헬스체크만 실패 | 앱이 뜨다 죽었다. 같은 실행의 로그 수집 스텝에 앱 로그 100줄이 있다 |

배포 job 자체가 목록에 없거나 skip 이면 워크플로 조건 문제다.
`if:` 를 `>-` 로 여러 줄에 쓸 때 **모든 줄의 들여쓰기를 맞춰야 한다.**
더 깊이 들여쓴 줄은 접히지 않고 줄바꿈이 남아 조건식이 깨진다.

---

## 🔴 SameSite 주의

Refresh Token 쿠키가 여기 걸린다.

| 프론트 ↔ 백엔드 | 같은 사이트? | `REFRESH_COOKIE_SAME_SITE` |
|---|---|---|
| **`getit.io.kr` ↔ `api.getit.io.kr`** | ✅ | **`Lax` ← 지금 구성** |
| `getit.vercel.app` ↔ `api.getit.io.kr` | ❌ | `None` |
| `localhost:5173` ↔ `api.getit.io.kr` | ❌ | `None` |

**`Lax` 인데 교차 사이트면 재발급 요청에 쿠키가 실리지 않아 로그인이 유지되지 않는다.**
증상이 "로그인은 되는데 새로고침하면 풀린다" 로 나타나서 원인을 찾기 어렵다.

`None` 은 `Secure=true` 가 함께여야 브라우저가 받는다. HTTPS 를 붙였으므로 문제없다.

---

## 도메인 구성 (적용 완료)

```
getit.io.kr        →  프론트엔드 (별도 저장소)
api.getit.io.kr    →  백엔드 (이 VM, 40.82.154.5)
```

DNS 는 Cloudflare 가 관리한다. 같은 등록 도메인이라 `SameSite=Lax` 를 쓴다.

⚠️ **Cloudflare 프록시를 켤 때는 SSL/TLS 모드를 `Full (strict)` 로 둔다.**
`Flexible` 이면 Cloudflare 가 VM 에 http 로 붙는데 nginx 가 https 로 되돌려 보내
무한 리다이렉트가 된다.

**도메인을 바꾸게 되면**

```bash
# 1. Cloudflare DNS 에 A 레코드 추가 → 40.82.154.5
# 2. 인증서 재발급 (nginx 구성도 함께 갱신된다)
sudo bash issue-cert.sh {새 도메인} getit0official@gmail.com
# 3. /opt/getit/.env 의 CORS_ALLOWED_ORIGINS · OAUTH2_REDIRECT_URI · FILE_BASE_URL 수정
cd /opt/getit && docker compose up -d
# 4. Google Console 리디렉션 URI 추가 → https://{새 도메인}/login/oauth2/code/google
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

`image_tag` 를 채우면 이미지 빌드를 건너뛴다. 롤백인데 빌드하면
`latest` 가 되돌리려는 버전이 아니라 최신 main 을 가리키게 된다.
배포한 이미지는 `.env` 의 `APP_IMAGE` 에 기록되므로, VM 에서 수동으로 재시작해도
되돌린 버전이 그대로 뜬다.

**리허설 결과 (V18 스키마에 V14 시절 이미지)** — 로컬에서 왕복 검증했다.

```
Successfully validated 18 migrations
WARN: Schema `getit` has a version (18) that is newer than
      the latest available migration (14) !
Schema `getit` is up to date. No migration necessary.
```

Flyway 는 경고만 남기고 진행한다. `ddl-auto: validate` 도 통과했고 앱은 정상 기동했다.
롤백은 코드만 되돌리고 스키마는 건드리지 않는다 — 그래서 다시 최신으로 올릴 때
마이그레이션 재실행 없이 바로 복귀한다.

⚠️ **다만 이건 V15~V18 에 파괴적 변경이 없었기 때문이다.**
V15·V18 은 테이블 추가, V16 은 인덱스 추가, V17 은 **nullable 컬럼 추가와 데이터 갱신**이다.
기존 컬럼을 지우거나 타입을 바꾼 것이 없어서 구버전 엔티티가 그대로 통과했다.
다음 경우엔 롤백이 깨진다.

| 마이그레이션 | 롤백 시 |
|---|---|
| 테이블 · 컬럼 추가 | 안전 (검증됨) |
| 컬럼 삭제 · 타입 변경 | 구버전 엔티티가 찾는 컬럼이 없어 `validate` 실패 |
| `NOT NULL` 제약 추가 | 구버전이 그 컬럼을 모르고 INSERT 하면 실패 |

**스키마를 파괴적으로 바꾸는 배포는 롤백이 안 된다고 보고 나눠서 배포한다.**

**DB 백업** — 매일 04:00 (KST) 자동 실행된다. `/opt/getit/backups` 에 14일 보관한다.

```bash
# 수동 실행
cd /opt/getit && ./backup-db.sh

# 백업 목록과 로그
ls -lh /opt/getit/backups/
tail /opt/getit/backups/backup.log
```

**복구** — 백업만 있고 복구 절차를 모르면 정작 필요할 때 못 쓴다.

```bash
cd /opt/getit
source .env

# 1. 앱을 먼저 내린다. 복구 중에 쓰기가 들어오면 데이터가 섞인다
docker compose stop app

# 2. 복구할 파일을 고른다
ls -lh backups/

# 3. 밀어넣는다
gunzip -c backups/getit-{타임스탬프}.sql.gz \
  | docker compose exec -T mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$DB_NAME"

# 4. 앱을 올린다
docker compose start app
docker compose logs app --tail 50
```

⚠️ **Flyway 와의 관계** — 백업에는 `flyway_schema_history` 도 들어 있다.
백업 시점보다 앞선 마이그레이션이 적용된 이미지가 떠 있으면
`ddl-auto: validate` 가 스키마 불일치로 기동을 막는다.
그럴 때는 백업 시점에 맞는 이미지 태그로 함께 롤백한다.

### 백업 오프디스크 복사

백업이 DB 와 같은 디스크에만 있으면 디스크가 깨질 때 둘 다 사라진다.
`upload-backup.sh` 가 백업 성공 직후 Azure Blob 으로 사본을 보낸다.

**자격증명을 저장하지 않는다.** VM 의 관리 ID 로 토큰을 받아 쓴다.
스토리지 키를 `.env` 에 넣으면 파일 하나가 새는 것만으로 어디서든 쓸 수 있는
장기 자격증명이 유출된다. 관리 ID 토큰은 VM 안에서만 받을 수 있고 곧 만료된다.

🔴 **다만 관리 ID 만으로 VM 침해가 막히지는 않는다.**
`Storage Blob Data Contributor` 에는 **삭제 권한이 포함돼 있다.**
VM 을 잡은 쪽은 IMDS 에서 토큰을 계속 받아 원격 백업도 지울 수 있다.
그래서 **soft delete 와 버전 관리를 반드시 함께 켠다.** 이 설정은 계정 수준이라
데이터 평면 권한(Blob Data Contributor)으로는 끌 수 없다. 지워져도 되돌릴 수 있다.

준비는 Azure 포털에서 한 번만 하면 된다.

1. **스토리지 계정 생성** — 리소스 그룹 `GETIT_그룹`, 지역 `Korea Central`
   - 중복성: `LRS` 로 충분하다
   - 🔴 **공용 액세스는 반드시 비활성화.** 백업에 지원자 개인정보가 들어간다
2. **데이터 보호 켜기** — 스토리지 계정 → 데이터 관리 → 데이터 보호
   - ✅ Blob 일시 삭제 (soft delete) — 보존 `30일`
   - ✅ 컨테이너 일시 삭제 — 보존 `30일`
   - ✅ Blob 버전 관리
   - 이걸 켜야 위의 삭제 위협이 실제로 막힌다. 건너뛰면 안 된다
3. **컨테이너 생성** — 이름 `db-backups`, 액세스 수준 **비공개**
4. **VM 관리 ID 켜기** — 가상 머신 → 보안 → ID → 시스템 할당 → **켬**
5. **역할 부여** — 스토리지 계정 → 액세스 제어(IAM) → 역할 할당 추가
   → 역할 `Storage Blob Data Contributor` → 액세스 할당 대상 `관리 ID` → VM 선택

   더 조이려면 `.../blobs/write` 와 `/read` 만 갖고 `/delete` 는 없는 **사용자 지정 역할**을
   만들어 부여한다. 그러면 VM 이 털려도 원격 백업을 지울 수 없다.
   변경 불가(immutability) 정책을 잠가서 걸면 더 강하지만, 잘못 걸면 보존 기간이 끝날 때까지
   아무도 못 지우므로 신중히 한다.

6. **VM 설정**

```bash
nano /opt/getit/.env
#   AZURE_STORAGE_ACCOUNT={만든 계정 이름}
#   AZURE_BACKUP_CONTAINER=db-backups

# 스크립트 설치 (백업 · 업로드 · 설치 세 개를 함께 보낸다)
scp -i GETIT_key.pem deploy/backup-db.sh deploy/upload-backup.sh deploy/install-backup.sh \
  azureuser@40.82.154.5:/tmp/
ssh -i GETIT_key.pem azureuser@40.82.154.5 'sudo bash /tmp/install-backup.sh'

# 바로 확인
ssh -i GETIT_key.pem azureuser@40.82.154.5 'cd /opt/getit && ./backup-db.sh'
```

마지막 줄에 `원격 복사 완료: db-backups/getit/2026/08/...` 가 나오면 된다.

`AZURE_STORAGE_ACCOUNT` 가 비어 있으면 원격 복사를 건너뛰고 로컬 백업만 한다.
원격 복사가 실패해도 로컬 백업은 남고, 로그에 실패가 기록된다.

**원격 보관 기간**은 스토리지 계정의 수명 주기 관리 정책으로 건다.
스크립트는 원격 파일을 지우지 않는다 — 백업을 지우는 코드는 버그가 나면 되돌릴 수 없다.

### 업로드 파일 저장소

스토리지 계정을 **셋으로 나눈다.** 보존 정책과 공개 여부가 서로 다르기 때문이다.

| 계정 | 컨테이너 | 공개 | 보존 |
|---|---|---|---|
| `getitbackup01` | `db-backups` | ❌ | **자동 삭제됨** — 수명 주기 정책 대상 |
| `getituploads01` | `uploads` | ❌ | 강의 자료 · 과제 제출물. **자동 삭제 금지** |
| `getitpublic01` | `public-assets` | ✅ | 프로필 이미지 · 프로젝트 썸네일 |

**왜 백업과 업로드를 나눴나** — 수명 주기 정책은 계정 단위다. 한 계정에 두면
백업 정리 규칙의 접두어를 잘못 적었을 때 과제 제출물이 사라진다. 되돌릴 수 없는 실수다.

**왜 공개용을 나눴나** — 공개를 허용하는 순간 그 계정의 다른 컨테이너도 공개로 바꿀 수
있는 상태가 된다. 비공개 계정은 계정 수준에서 잠겨 있어 설정 실수로도 열리지 않는다.

⚠️ 계정을 나눠도 **권한 분리는 되지 않는다.** 백업 스크립트와 앱이 같은 VM 의 같은
관리 ID 를 쓴다. 그건 soft delete 와 버전 관리가 막는 부분이다.

**어디에 저장되는지는 `FilePurpose` 가 정한다.** 저장 키가 `public/` · `private/` 로
시작해서, 키만 봐도 어느 저장소인지 알 수 있다.

```
LECTURE_MATERIAL · ASSIGNMENT      → private/{uuid}.ext  서명 주소 5분
PROFILE_IMAGE · PROJECT_THUMBNAIL  → public/{uuid}.ext   고정 주소, 캐시 가능
```

`FILE_AZURE_ENABLED=true` 면 프론트가 Blob 으로 **직접** 올린다. 파일 바이트가 VM 을
지나가지 않으므로 50MB 자료가 몰려도 애플리케이션이 영향을 받지 않고, 디스크가 깨져도
파일이 사라지지 않는다.

브라우저가 직접 올리려면 스토리지 계정에 CORS 가 필요하다. 세 계정 모두 설정돼 있다.

```
허용 출처  https://getit.io.kr · https://www.getit.io.kr · http://localhost:5173
허용 메서드 PUT · GET · HEAD · OPTIONS
```

프론트 주소가 바뀌면 CORS 도 함께 고쳐야 한다. 빠뜨리면 업로드만 조용히 실패한다.

`false` 로 두면 VM 로컬 디스크에 저장한다. **이때는 파일이 백업되지 않는다.**

**켜는 절차**

```bash
nano /opt/getit/.env
#   FILE_AZURE_ENABLED=true
#   AZURE_UPLOAD_ACCOUNT=getituploads01
#   AZURE_PUBLIC_ACCOUNT=getitpublic01
cd /opt/getit && docker compose up -d
```

## 아직 남은 것

| 항목 | 담당 | 비고 |
|---|---|---|
| 백업 오프디스크 복사 | R | 스크립트는 있다. Azure 스토리지 계정 · 관리 ID 설정만 남았다 |
| 모니터링 | B | 헬스체크 외에 알림 없음 (UptimeRobot 정도면 충분) |
| Cloudflare 프록시 | R | 켜려면 SSL/TLS 를 `Full (strict)` 로. `Flexible` 은 리다이렉트 루프 |
| Redis | — | **만들지 않는다.** 코드에서 쓰지 않는다 |
| 프론트 배포 | R | 별도 저장소 |
