# GETIT Site — Backend

GETIT 동아리 통합 사이트 백엔드. 공개 사이트 · 부원 LMS · 운영진 어드민 세 영역을 하나의 API 서버가 담당한다.

설계 문서는 `DOCS/` 참조 — 백엔드 설계 명세서, API 명세서(102개 엔드포인트), 작업 분할 계획, 코딩 컨벤션.

## 기술 스택

| 항목 | 버전 |
|---|---|
| Java | 21 (toolchain 고정) |
| Spring Boot | 3.5.16 |
| Gradle | Wrapper 사용 (`./gradlew`) |
| DB | MySQL 8.4 / 테스트는 H2 |
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
./gradlew test                # 테스트 (외부 인프라 불필요, H2 사용)
./gradlew build               # 빌드 + 테스트
```

### 환경변수 (.env)

DB 계정 등 자격증명은 **저장소에 커밋하지 않는다.** `.env.example` 을 복사해 각자 `.env` 를 만든다.

| 환경 | 값을 읽는 곳 |
|---|---|
| local | `.env` — docker compose 가 직접 읽고, 애플리케이션은 `spring.config.import` 로 읽는다 |
| dev · prod | 실제 환경변수 (GitHub Actions Secrets · 서버 환경변수) |

`.env` 가 없으면 `Config data resource 'file [.env]' ... does not exist` 로 즉시 실패한다.
테스트는 H2 를 쓰므로 `.env` 없이도 돌아간다 (CI 가 이 경로를 쓴다).

## 패키지 구조

```
com.getit
├── global                    R — 전 도메인 공통. 소유자 외 수정 금지
│   ├── config                SecurityConfig · CorsConfig · OpenApiConfig · JpaAuditingConfig
│   ├── dto                   ApiResponse · ErrorResponse · PageResponse
│   ├── entity                BaseTimeEntity · SoftDeletableEntity
│   └── exception             ErrorCode · CommonErrorCode · BusinessException · GlobalExceptionHandler
└── domain
    ├── auth                  R   OAuth2 · JWT
    ├── user                  A   사용자 · 그룹
    ├── recruitment           A   기수 · 지원서 · 평가
    ├── dashboard             A   운영진 통계 (조립만)
    ├── setting               A/B 하위 패키지 단위로 분할
    │   ├── generation        A
    │   ├── curriculum        A
    │   ├── staff             A
    │   ├── home              A
    │   ├── category          B
    │   ├── event             B
    │   ├── faq               B
    │   └── feature           B
    ├── lecture               B   분류 · 강의 · 과제 · 제출 · 피드백
    ├── qna                   B   질문 · 답변
    ├── project               B   프로젝트 쇼케이스
    └── file                  B   공통 파일 업로드
```

**패키지 = 소유권.** 자기 패키지 밖의 파일은 수정하지 않고 소유자에게 요청한다.
각자 Controller → Service → Repository → Entity 를 자기 패키지 안에서 끝낸다.
B 이탈 후 `lecture` · `qna` · `project` · `file` 은 R 이 함께 관리한다.

### 왜 레이어가 아니라 도메인으로 나누는가

`controller/` · `service/` · `repository/` 를 최상위에 두는 레이어 분할이라면
기능 하나를 고칠 때마다 서로 다른 네 디렉터리를 건드리게 된다.
여러 명이 동시에 작업하는 저장소에서는 그 네 디렉터리가 전부 공유 지점이 되어
**서로 무관한 기능끼리도 PR 마다 충돌한다.**

도메인으로 나누면 기능 하나가 디렉터리 하나 안에서 끝나므로
소유권 경계와 디렉터리 경계가 일치하고, 충돌 지점이 `global` 과
도메인 간 계약 두 곳으로 좁혀진다. 그 대가로 도메인끼리 Repository 를
직접 참조하면 경계가 곧바로 무너지기 때문에, 아래 크로스 도메인 규칙이
선택이 아니라 이 구조의 전제다.

### 절대 건드리지 않는 파일 (작업 분할 계획 4.1)

| 파일 | 소유 |
|---|---|
| `global/config/SecurityConfig.java` | R — 경로 규칙 추가는 R 에게 요청 |
| `src/main/resources/application*.yml` | R — 설정 추가는 R 에게 요청 |
| `global/dto/*`, `global/exception/GlobalExceptionHandler.java` | R — 수정 금지 |
| `User` · `Generation` 엔티티 | A — B 는 읽기만, 필드 추가는 A 에게 요청 |

### 크로스 도메인 참조 (작업 분할 계획 4.2)

다른 도메인의 Repository 를 직접 참조하지 않는다. **제공자 패키지에 인터페이스를 두고 소비자가 주입받는다.**
`UserQueryService`(A 제공), `LectureStatService` · `QuestionStatService` · `EventQueryService` · `HomeContentProvider`(B 제공).

## 공통 규약

### 응답 envelope

모든 응답은 `ApiResponse<T>` 로 감싼다. (API 명세서 0.2)

```json
{ "success": true,  "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "APPLICATION_DEADLINE_PASSED", "message": "..." } }
```

`fieldErrors` 는 `@Valid` 검증 실패(`VALIDATION_FAILED`) 시에만 포함된다.
`null` 은 전역으로 생략하지 않는다 — 명세서가 `null` 을 의미 있는 값으로 쓴다 (`totalScore: null` = 미평가).

### ErrorCode

한 enum 에 몰면 PR 마다 충돌하므로 도메인별 파일로 쪼갠다. `global.exception.ErrorCode` 인터페이스를 구현하면 된다.

```java
public enum RecruitmentErrorCode implements ErrorCode { ... }
```

### 시간

서버 타임존은 `Asia/Seoul` 고정 (JVM · Jackson · Hibernate JDBC 3중). D-day 는 항상 서버에서 계산해 내려준다.

## 브랜치 · 커밋

```
브랜치   feat/{이슈번호}-{작업내용}     예) feat/12-application-submit
커밋     feat(recruitment): 지원서 임시저장 API 구현
PR       프로덕션 500줄 이하. 교차 리뷰 1 approve 로 머지. main 직접 push 금지
```

Branch type: `feat` · `fix` · `refactor` · `chore` — 1 Issue 1 Branch, PR 본문에 `close #이슈번호`.

### PR 크기

**프로덕션 코드 500줄 이하.** 테스트는 세지 않는다.

리뷰 부하는 프로덕션 코드에서 나오고, 테스트는 오히려 리뷰를 쉽게 만든다.
전체 변경으로 세면 테스트를 줄일 유인이 생겨 규약이 거꾸로 작동한다.

```bash
git diff --numstat main...HEAD -- src/main | awk '{s+=$1} END {print s" 줄"}'
```

이슈 · PR 템플릿은 `.github/` 에 있다. 이슈를 만들면 종류에 맞는 양식이 자동으로 뜬다.

## CI

`main` 으로의 push · PR 마다 `.github/workflows/ci.yml` 의 세 job 이 돈다.

| job | 하는 일 |
|---|---|
| 정적 검사 | `.env` 가 커밋되지 않았는지 확인 → `./gradlew spotlessCheck` |
| 빌드 · 테스트 | `./gradlew build` (H2 라 컨테이너 불필요). 실패 시 테스트 리포트 업로드 |
| 스키마 검증 | `./gradlew schemaTest` — Flyway 마이그레이션과 엔티티가 어긋나지 않는지 확인 |

같은 브랜치에 연속 push 하면 이전 실행은 취소된다.

`main` 브랜치 보호 규칙에서 이 체크를 필수로 걸어두면 깨진 코드가 머지되지 않는다.

## 현재 상태

전 도메인이 구현돼 운영 중이다. `api.getit.io.kr` 로 배포되며,
`main` 에 머지되면 `.github/workflows/cd.yml` 이 이미지를 빌드해 Azure VM 에 올린다.

인증은 Google OAuth2 로그인 + JWT 로 동작한다 (`domain.auth`).
`SecurityConfig` 는 `/api/public/**` 과 문서 경로만 열어 두고,
`/api/admin/**` 은 ADMIN 롤, 그 외는 인증을 요구한다.

스키마는 dev · prod 에서 Flyway 로 관리한다. local 만 `ddl-auto: update` 다.
