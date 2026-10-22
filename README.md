# GETIT Site — Backend

GETIT 동아리 통합 사이트 백엔드. 공개 사이트 · 부원 LMS · 운영진 어드민 세 영역을
하나의 API 서버가 담당한다. `api.getit.io.kr` 로 배포돼 운영 중이다.

> **처음 왔다면 [docs/ONBOARDING.md](docs/ONBOARDING.md) 부터 읽는다.**
> 띄우기 → 코드 한 조각 따라가기 → 첫 PR 까지 순서대로 안내한다.

## 문서 지도

| 문서 | 무엇에 답하나 |
|---|---|
| [ONBOARDING.md](docs/ONBOARDING.md) | 처음 투입됐다. 무엇부터 하면 되나 |
| [GLOSSARY.md](docs/GLOSSARY.md) | 기수 · 활성 기수 · 트랙 … 이 말이 무슨 뜻인가 |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | 왜 이 구조인가. 내 코드를 어디에 둬야 하나 |
| [CONVENTIONS.md](docs/CONVENTIONS.md) | 이렇게 써도 되나 |
| [PITFALLS.md](docs/PITFALLS.md) | 자주 나는 사고 + **PR 전 리뷰 체크리스트** |
| [WORKFLOW.md](docs/WORKFLOW.md) | 이슈 → PR → 머지 → 배포 |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) | 배포 · 운영 · 장애 대응 |
| [adr/](docs/adr/) | 왜 그렇게 결정했나 |
| [db/migration/README.md](src/main/resources/db/migration/README.md) | 마이그레이션 번호 예약 표 |

**API 목록은 문서로 관리하지 않는다.** 앱을 띄우고
<http://localhost:8080/swagger-ui.html> 를 본다 — 손으로 쓴 목록은 반드시 낡는다.

기획 · 정책 문서(요구사항 · UI/UX · 운영 방침)는 노션의 **GETIT SITE** 페이지에 있다.
**개발 문서의 기준은 이 저장소**이고, 노션에는 복사하지 않는다.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 21 (toolchain 고정) |
| Spring Boot | 3.5.16 |
| Gradle | Wrapper 사용 (`./gradlew`) |
| DB | MySQL 8.4 / 테스트는 H2 |
| 마이그레이션 | Flyway (dev · prod. local 은 `ddl-auto: update`) |
| 인증 | Spring Security + OAuth2(Google) + JWT |
| 문서 | SpringDoc OpenAPI 3 |

로컬 JDK 버전은 상관없다. Gradle toolchain 이 Java 21 로 컴파일하며, 없으면 자동으로 받아온다.

## 시작하기

```bash
cp .env.example .env          # 값을 채운다. .env 는 커밋하지 않는다
docker compose up -d          # MySQL 기동
./gradlew bootRun             # 기본 프로파일 = local
```

| 주소 | 용도 |
|---|---|
| http://localhost:8080 | API |
| http://localhost:8080/swagger-ui.html | Swagger UI |

`.env.example` 을 복사해 DB 비밀번호와 `JWT_SECRET` 만 채우면 앱은 뜬다.
Google 값은 주석 처리된 채로 두면 되고, 그 상태에서 되는 것과 안 되는 것은 다음과 같다.

| | 상태 |
|---|---|
| 기동 · Swagger · 공개 API | 된다 |
| 파일 업로드 | 된다 — `FILE_AZURE_ENABLED` 가 꺼져 있어 `./uploads` 에 저장한다 |
| Google 로그인 | **안 된다** — `GOOGLE_CLIENT_ID` · `GOOGLE_CLIENT_SECRET` 주석을 풀고 값을 채워야 한다 |

주석을 풀고 **빈 값으로 두면 기동 자체가 실패한다.** 채울 때만 주석을 해제한다.

```bash
./gradlew test            # 테스트 (외부 인프라 불필요, H2 사용)
./gradlew build           # 빌드 + 테스트
./gradlew schemaTest      # 마이그레이션 ↔ 엔티티 일치 검증 (MySQL 필요)
./gradlew spotlessApply   # 포맷 자동 수정
```

### 환경변수 (.env)

DB 계정 등 자격증명은 **저장소에 커밋하지 않는다.** `.env.example` 을 복사해 각자 `.env` 를 만든다.

| 환경 | 값을 읽는 곳 |
|---|---|
| local | `.env` — docker compose 가 직접 읽고, 애플리케이션은 `spring.config.import` 로 읽는다 |
| dev · prod | 실제 환경변수 (GitHub Actions Secrets · 서버 환경변수) |

`.env` 가 없으면 `Config data resource 'file [.env]' ... does not exist` 로 즉시 실패한다.
테스트는 H2 를 쓰므로 `.env` 없이도 돌아간다 (CI 가 이 경로를 쓴다).

## 현재 상태

전 도메인이 구현돼 운영 중이다. `main` 에서 CI 가 성공하면
`.github/workflows/cd.yml` 이 이미지를 빌드해 Azure VM 에 올린다.

인증은 Google OAuth2 로그인 + JWT 로 동작한다 (`domain.auth`).
`SecurityConfig` 는 `/api/public/**` 과 문서 경로만 열어 두고,
`/api/admin/**` 은 ADMIN 롤, 그 외는 인증을 요구한다.

스키마는 dev · prod 에서 Flyway 로 관리한다. local 만 `ddl-auto: update` 다.

구조와 규약은 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) ·
[docs/CONVENTIONS.md](docs/CONVENTIONS.md) 를 본다.
