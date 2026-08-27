# 배포 가이드 (Azure App Service)

## 지금 상태

| 단계 | 상태 |
|---|---|
| Dockerfile | ✅ 있음. 로컬에서 빌드 · 기동 검증 완료 |
| 이미지 빌드 · GHCR 푸시 | ✅ `main` 머지마다 자동 |
| Azure 배포 | ⬜ **비활성.** 리소스와 변수를 준비하면 켜진다 |

`main` 에 머지하면 이미지가 GHCR 에 쌓인다. Azure 계정이 없어도 동작한다.
배포 job 은 `AZURE_DEPLOY_ENABLED` 변수가 `true` 일 때만 실행된다.

```
ghcr.io/getit-knu/getit_site_be:latest
ghcr.io/getit-knu/getit_site_be:sha-{커밋}
```

---

## 🔴 배포 전에 반드시 해결할 것

### 1. 업로드 파일이 사라진다

`LocalFileStorage` 가 컨테이너 로컬 디스크(`./uploads`)에 파일을 쓴다.
App Service 는 **컨테이너 파일시스템이 휘발성**이라 재시작 · 재배포마다 전부 사라진다.

강의 자료 · 과제 제출물 · 운영진 프로필 이미지 · 프로젝트 썸네일이 모두 여기 걸린다.

**해결** — `AzureBlobFileStorage` 구현체 추가 (B 담당).
`FileStorage` 인터페이스가 이미 있고 코드 주석에도 예정으로 적혀 있다.

```java
// LocalFileStorage.java
// 추후 Azure Blob File Storage 추가 시 ConditionalOnProperty 어노테이션 사용
```

임시로 넘기려면 App Service 에 Azure Files 를 마운트하고 `FILE_LOCAL_PATH` 를 그 경로로 지정할 수도 있다.
느리고 관리 포인트가 늘어나므로 Blob 전환 전까지의 임시 방편으로만 쓴다.

### 2. Redis 는 만들지 않는다

`spring-boot-starter-data-redis` 의존성은 있으나 **코드에서 전혀 쓰지 않는다.**
Refresh Token 은 DB 테이블에 저장한다.

**Azure Cache for Redis 를 프로비저닝하지 말 것.** 최저 티어도 월 $16 수준인데 쓰는 곳이 없다.
헬스체크에서도 제외해 두었다 (`management.health.redis.enabled: false`).

실제로 쓰게 되면 그때 만들고 헬스 인디케이터를 다시 켠다.

### 3. Google OAuth 리디렉션 URI

배포 도메인이 정해지면 Google Cloud Console 의 승인된 리디렉션 URI 에 추가해야 한다.

```
https://{앱이름}.azurewebsites.net/login/oauth2/code/google
```

빠뜨리면 로그인이 `redirect_uri_mismatch` 로 실패한다.

---

## Azure 리소스

### 만들 것

| 리소스 | 종류 | 비고 |
|---|---|---|
| App Service Plan | Linux, B1 이상 | 월 $13 수준. F1(무료)은 always-on 이 없어 콜드스타트가 생긴다 |
| App Service | 컨테이너 (Linux) | 아래 설정 참조 |
| Azure Database for MySQL | Flexible Server, Burstable B1ms | 월 $12 수준 |
| Storage Account | Blob | 파일 업로드용. 위 1번 해결 시 |

**만들지 않을 것** — Azure Cache for Redis (위 2번), ACR (GHCR 을 쓴다)

### App Service 설정

**컨테이너**

```
레지스트리 : ghcr.io  (Private registry)
이미지     : ghcr.io/getit-knu/getit_site_be:latest
```

GHCR 이 private 이면 App Service 에 자격증명이 필요하다.
GitHub 에서 `read:packages` 권한만 가진 PAT 를 만들어 사용자명 · 비밀번호로 넣는다.

**일반 설정**

```
상태 검사 경로 : /actuator/health
Always On     : 켬
HTTPS 만 허용  : 켬
```

상태 검사를 설정하지 않으면 컨테이너가 죽어도 트래픽이 계속 들어간다.

**애플리케이션 설정 (환경변수)**

| 이름 | 값 | 비고 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | |
| `WEBSITES_PORT` | `8080` | App Service 에 컨테이너 포트를 알려준다. 없으면 502 |
| `DB_HOST` | `{서버}.mysql.database.azure.com` | |
| `DB_PORT` | `3306` | |
| `DB_NAME` | `getit` | |
| `DB_USERNAME` | | |
| `DB_PASSWORD` | | |
| `JWT_SECRET` | `openssl rand -base64 48` | 로컬 값을 재사용하지 말 것 |
| `GOOGLE_CLIENT_ID` | | |
| `GOOGLE_CLIENT_SECRET` | | |
| `CORS_ALLOWED_ORIGINS` | 프론트 배포 주소 | |
| `OAUTH2_REDIRECT_URI` | `https://{프론트}/oauth/callback` | |
| `REFRESH_COOKIE_SECURE` | `true` | **필수.** false 면 쿠키가 http 로 나간다 |
| `FILE_BASE_URL` | `https://{앱이름}.azurewebsites.net/api/public/files` | Blob 전환 전까지 |

`REDIS_HOST` 는 넣지 않는다. 위 2번 참조.

### MySQL

- 첫 배포 시 DB 는 **비어 있어야 한다.** Flyway 가 `V1` 부터 적용한다
- 네트워킹에서 **"Azure 서비스의 액세스 허용"** 을 켜거나 App Service 아웃바운드 IP 를 방화벽에 추가한다
- 인스턴스를 여러 대로 늘리면 마이그레이션이 동시에 돌 수 있다. Flyway 가 잠금으로 직렬화하지만
  **첫 배포는 1대로 올린다**

---

## GitHub Actions ↔ Azure 연결 (OIDC)

장기 자격증명을 저장소에 두지 않는 방식이다. 게시 프로필(publish profile)보다 안전하다.

### 1. Azure 쪽

```bash
# 앱 등록
az ad app create --display-name "getit-be-github-actions"
# 반환된 appId 를 기록 → AZURE_CLIENT_ID

# 서비스 주체 생성
az ad sp create --id {appId}

# 리소스 그룹에 배포 권한 부여
az role assignment create \
  --role "Contributor" \
  --subscription {subscriptionId} \
  --assignee-object-id {sp objectId} \
  --assignee-principal-type ServicePrincipal \
  --scope /subscriptions/{subscriptionId}/resourceGroups/{리소스그룹}
```

**페더레이션 자격증명 추가** — Azure Portal → 앱 등록 → 인증서 및 비밀 → 페더레이션 자격 증명

```
시나리오   : GitHub Actions deploying Azure resources
조직       : getit-knu
리포지토리 : GETIT_Site_BE
엔터티     : Environment
환경 이름  : production
```

`environment: production` 을 워크플로에 지정했으므로 엔터티를 Environment 로 맞춰야 한다.
Branch 로 만들면 인증이 실패한다.

### 2. GitHub 쪽

Settings → Secrets and variables → Actions

**Secrets**

```
AZURE_CLIENT_ID
AZURE_TENANT_ID
AZURE_SUBSCRIPTION_ID
```

**Variables**

```
AZURE_DEPLOY_ENABLED = true      ← 이 값이 true 가 되는 순간 배포가 시작된다
AZURE_WEBAPP_NAME    = {App Service 이름}
```

Settings → Environments → `production` 생성.
승인자를 지정하면 배포 전에 사람이 승인하도록 만들 수 있다.

---

## 배포 흐름

```
main 머지
   ↓
CI       정적 검사 · 빌드/테스트 · 스키마 검증
   ↓
CD [1]   이미지 빌드 → GHCR 푸시 (sha-{커밋}, latest)
   ↓
CD [2]   AZURE_DEPLOY_ENABLED == true 일 때만
         Azure 로그인(OIDC) → App Service 이미지 교체 → 헬스체크 5분 대기
```

헬스체크가 통과해야 배포가 성공으로 기록된다. 실패하면 워크플로가 빨간불이 되므로
죽은 인스턴스가 조용히 배포되는 일이 없다.

## 롤백

이미지에 커밋 SHA 태그가 붙어 있다.

Actions → CD → Run workflow → `image_tag` 에 이전 태그 입력

```
sha-{되돌릴 커밋의 전체 SHA}
```

또는 Azure Portal 에서 컨테이너 이미지 태그를 직접 바꿔도 된다.

**주의** — 마이그레이션은 되돌아가지 않는다. `V{n}` 이 적용된 뒤 이전 이미지로 롤백하면
`ddl-auto: validate` 가 스키마 불일치로 기동을 막을 수 있다.
스키마를 바꾸는 배포는 롤백이 어렵다는 점을 감안해서 나눠 배포한다.

## 비용 개요

| 항목 | 월 예상 |
|---|---|
| App Service Plan B1 | ~$13 |
| Azure Database for MySQL B1ms | ~$12 |
| Blob Storage | 사용량. 소규모면 $1 미만 |
| GHCR | 무료 |
| **합계** | **~$25** |

Azure for Students 크레딧($100)으로 4개월 정도 감당된다.
Redis 를 만들면 여기에 월 $16 이 추가되는데 **쓰는 곳이 없다.**
