# 구조

왜 이렇게 생겼는지와, 요청 하나가 어떤 길을 지나는지.

처음 읽는다면 [ONBOARDING.md](ONBOARDING.md) 를 먼저 보는 쪽이 빠르다.
이 문서는 "왜 이 구조인가" 를 설명하고, 거기서는 실제 코드를 따라간다.

---

## 세 영역과 경로 권한

하나의 API 서버가 성격이 다른 세 영역을 담당한다. 권한은 **경로 prefix 로 갈린다.**
규칙은 `global/config/SecurityConfig.java` 한 곳에만 있다.

| 경로 | 권한 |
|---|---|
| `/api/public/**` | 누구나 |
| `/api/member/**` | `MEMBER` 또는 `ADMIN` |
| `/api/admin/**` | `ADMIN` |
| `/api/applications/**` | 로그인만 — **`GUEST` 도 된다** |
| `/api/files/**` | 로그인만 |
| 그 밖의 모든 경로 | 로그인 필요 |

**`api/v1` 같은 버전 prefix 는 쓰지 않는다.** 경로는 `/api/...` 로 시작한다.

`/api/applications/**` 가 `GUEST` 를 허용하는 게 핵심이다. **지원자는 아직 부원이 아니다.**
여기에 `MEMBER` 를 요구하면 아무도 지원할 수 없다.

인증 없이 열린 경로가 몇 개 더 있고, 각각 이유가 있다.

| 경로 | 왜 여는가 |
|---|---|
| `/oauth2/**` · `/login/oauth2/**` | Google 로그인 리다이렉트와 콜백. 로그인 전이라 인증을 걸 수 없다 |
| `/api/auth/refresh` · `/logout` · `/callback` | Access Token 이 **만료된 상태에서** 호출된다. 로그아웃에 인증을 걸면 만료된 사용자가 로그아웃을 못 해 Refresh 가 최대 2주 살아남는다 |
| `/actuator/health` | 배포 환경의 헬스 프로브. 인증을 걸면 플랫폼이 인스턴스를 죽은 것으로 판단한다 |
| Swagger · `/v3/api-docs` | 문서 |

---

## 패키지 구조

```
com.getit
├── global                    전 도메인 공통
│   ├── config                SecurityConfig · CorsConfig · OpenApiConfig · JpaAuditingConfig
│   ├── dto                   ApiResponse · ErrorResponse · PageResponse
│   ├── entity                BaseTimeEntity · SoftDeletableEntity
│   ├── exception             ErrorCode · CommonErrorCode · BusinessException · GlobalExceptionHandler
│   ├── time                  Clock 설정
│   └── validation            공통 검증
└── domain
    ├── auth                  OAuth2 · JWT · Refresh Token · 개인정보 동의
    ├── user                  사용자 · 조 · 단과대/전공
    ├── recruitment           기수 모집 일정 · 지원서 · 질문 · 평가 · 합불
    ├── lecture               강의 · 과제 · 제출 · 피드백
    │   └── admin             운영진용 강의 관리
    ├── qna                   질문 · 답변
    │   └── admin             운영진용 Q&A 관리
    ├── project               프로젝트 쇼케이스
    │   ├── admin             승인 · 반려
    │   └── member            부원 등록 · 조회
    ├── dashboard             운영진 통계 — 다른 도메인의 계약을 조립만 한다
    ├── file                  공통 파일 업로드 · 다운로드 주소 발급
    └── setting               사이트 설정 — 하위 패키지 단위로 나뉜다
        ├── generation        기수
        ├── category          트랙 · 소분류
        ├── curriculum        홈 커리큘럼
        ├── staff             운영진 소개
        ├── event             행사 일정
        ├── faq               FAQ
        ├── photo             활동 사진
        ├── feature           기능 토글
        └── home              홈 일괄 조회 · 저장
```

한 도메인이 운영진용과 사용자용을 함께 가질 때는 **`admin` · `member` 서브패키지로 나눈다**
(`lecture/admin` · `project/member`). 권한이 다른 코드가 한 디렉터리에 섞이지 않게 하는 장치다.

### 왜 레이어가 아니라 도메인으로 나누는가

`controller/` · `service/` · `repository/` 를 최상위에 두는 레이어 분할이라면
기능 하나를 고칠 때마다 서로 다른 네 디렉터리를 건드리게 된다.
여러 명이 동시에 작업하는 저장소에서는 그 네 디렉터리가 전부 공유 지점이 되어
**서로 무관한 기능끼리도 PR 마다 충돌한다.**

도메인으로 나누면 기능 하나가 디렉터리 하나 안에서 끝나므로
소유권 경계와 디렉터리 경계가 일치하고, 충돌 지점이 `global` 과
도메인 간 계약 두 곳으로 좁혀진다. 그 대가로 도메인끼리 Repository 를
직접 참조하면 경계가 곧바로 무너지기 때문에, 아래 크로스 도메인 규칙이
선택이 아니라 **이 구조의 전제**다.

### 한 도메인 안의 생김새

```
domain/recruitment
├── controller      HTTP 입출구. 얇다
├── service         비즈니스 규칙. 여기가 두껍다
├── repository      DB 접근
├── entity          JPA 엔티티 · enum
├── dto             요청/응답/Result 타입
├── exception       도메인 전용 ErrorCode
└── config          도메인 전용 설정 (있는 경우만)
```

---

## 요청 한 번의 흐름

```
HTTP 요청
   ↓  JWT 필터 — 토큰을 까서 인증 정보를 심는다            (domain/auth)
   ↓  SecurityConfig — 경로 prefix 로 권한 확인            (global/config)
   ↓  Controller — 요청 DTO 검증(@Valid), 서비스 호출      (얇게)
   ↓  Service — 규칙 검증 · 활성 기수 스코프 · 트랜잭션    (두껍게)
   ↓  Repository — 쿼리
   ↓  Entity — 상태 변경은 의미가 드러나는 메서드로
   ↑  Service 가 Result DTO 로 변환해서 반환
   ↑  Controller 가 ApiResponse<T> 로 감싼다
HTTP 응답
```

예외가 나면 어디서 터지든 `global/exception/GlobalExceptionHandler` 가 받아
같은 envelope 모양으로 바꿔 내려준다. 컨트롤러에서 try-catch 하지 않는다.

### 응답 envelope

모든 응답은 `ApiResponse<T>` 로 감싼다.

```json
{ "success": true,  "data": { }, "error": null }
{ "success": false, "data": null, "error": { "code": "APPLICATION_DEADLINE_PASSED", "message": "..." } }
```

`fieldErrors` 는 `@Valid` 검증 실패(`VALIDATION_FAILED`) 시에만 채운다.

**`null` 을 전역으로 생략하지 않는다.** 이 API 는 `null` 을 의미 있는 값으로 쓴다 —
`totalScore: null` 은 "0점" 이 아니라 "아직 평가하지 않음" 이다.

### 에러 코드

한 enum 에 전부 몰면 PR 마다 충돌하므로 **도메인별 파일로 쪼갠다.**
`global.exception.ErrorCode` 인터페이스를 구현하면 된다.

```java
public enum RecruitmentErrorCode implements ErrorCode { ... }
```

현재 도메인별 ErrorCode 는 16개다. `CommonErrorCode` 에 도메인 전용 코드를 넣지 않는다.

---

## 크로스 도메인 계약

다른 도메인의 데이터가 필요할 때 **그 도메인의 Repository 를 직접 주입받지 않는다.**
데이터를 가진 쪽에 인터페이스를 두고 소비자가 그걸 주입받는다.

```java
private final UserRepository userRepository;        // ❌ 금지
private final UserQueryService userQueryService;    // ✅ 계약 경유
```

이 프로젝트에서 `interface` + `Impl` 분리가 허용되는 **유일한 경우**다
(일반 서비스는 interface 를 두지 않는다 — [CONVENTIONS.md](CONVENTIONS.md#interface-를-만들지-않는다)).

현재 **26개**가 있고, **전부 다른 패키지에서 소비된다** — 내부에서만 쓰는
불필요한 인터페이스는 하나도 없다. 세는 명령:

```bash
git ls-files 'src/main/java/**/service/*.java' | xargs grep -l '^public interface' | wc -l
```

### 어디에 두는가

**데이터를 가진 쪽(제공자)에 둔다.** 26개 전부 그렇다.
`setting` 은 하위 패키지가 각자 소유 단위라, `setting/home` 이 `setting/faq` 의 계약을
쓰는 것도 같은 규칙을 따른다.

### 이름으로 쓰임이 갈린다

| 접미사 | 쓰임 | 예 |
|---|---|---|
| `...QueryService` | 읽기 | `GenerationQueryService` · `UserQueryService` |
| `...StatService` | 집계 | `LectureStatService` · `QuestionStatService` |
| `...BulkService` · `...WriteService` | 쓰기 | `FaqBulkService` · `RecruitmentScheduleWriteService` |
| 그 밖 | 특정 행위 | `FileConnectionService` · `LectureAccessService` · `UserAccountService` |

### 주요 계약

| 제공 | 계약 | 소비하는 곳 |
|---|---|---|
| `setting/generation` | `GenerationQueryService` | **9개 패키지** — 활성 기수를 알아야 하므로 거의 모든 곳 |
| `user` | `UserAccountService` | **5개 도메인** — auth · lecture · qna · recruitment · setting |
| `user` | `UserQueryService` | dashboard · lecture/admin · qna/admin · recruitment |
| `user` | `UserProfileService` · `CollegeQueryService` | auth · 이름 조회가 필요한 곳 |
| `user` | `MemberGroupService` | project |
| `recruitment` | `ApplicationQueryService` | dashboard · user(합격자 승격) |
| `recruitment` | `RecruitmentStatusQueryService` · `RecruitmentScheduleWriteService` | setting |
| `lecture` | `LectureStatService` · `LectureQueryService` | dashboard · qna |
| `lecture` | `LectureAccessService` | qna — 질문 권한 확인 |
| `lecture` | `CategoryLectureLinkService` | setting/category — 분류 삭제 전 강의 수 확인 |
| `qna` | `QuestionStatService` | dashboard |
| `project` | `ProjectQueryService` | setting/home |
| `file` | `FileQueryService` · `FileConnectionService` | **6개 패키지** — lecture · project · user · setting/home · photo · staff |
| `setting/*` | `...QueryService` · `...BulkService` | setting/home · dashboard |

소비처를 직접 확인하려면:

```bash
grep -rl GenerationQueryService src/main/java --include='*.java' | grep -v 'service/GenerationQueryService.java'
```

### 인터페이스는 소비자가 붙기 전에 완성한다

나중에 시그니처를 바꾸면 **양쪽 PR 을 동시에 건드려야 한다.**

특히 **목록을 다루는 소비자가 있으면 배치 조회 메서드를 처음부터 넣는다.**

```java
long countLecturesBySubCategoryId(Long id);                  // 단건만 두면
Map<Long, Long> countLecturesBySubCategoryIds(List<Long> ids); // 소비자 쪽에서 N+1 이 난다
```

`GenerationQueryService` 에 `findActive()` 와 `findActiveForWrite()` 가 따로 있는 것도
눈여겨볼 것 — 쓰기 작업은 활성화 전환과 경쟁할 수 있어 잠금이 필요하다.

`dashboard` 는 자기 엔티티가 없다. **위 계약들을 조립만 하는 도메인**이라
크로스 도메인 규칙이 실제로 어떻게 쓰이는지 보기에 가장 좋다.

---

## ArchUnit 이 강제하는 것

위 규칙 중 여섯 개는 **문서상의 약속이 아니라 테스트다.** 어기면 `./gradlew test` 가 깨진다.
`src/test/java/com/getit/architecture/ArchitectureTest.java`

| 규칙 | 막는 사고 |
|---|---|
| 다른 도메인의 repository · entity 를 직접 참조하지 않는다 | 도메인 경계 붕괴. enum 은 예외 |
| 컨트롤러는 repository 를 직접 호출하지 않는다 | 서비스를 건너뛴 쿼리 |
| 컨트롤러에 `@Transactional` 을 붙이지 않는다 | 트랜잭션 경계가 HTTP 레이어로 새는 것 |
| repository 는 service 를 참조하지 않는다 | 역방향 의존 |
| service 는 controller 를 참조하지 않는다 | 역방향 의존 |
| `global` 은 도메인의 구현을 참조하지 않는다 | 공통 패키지가 특정 도메인에 묶이는 것 |

마지막 규칙에 예외가 둘 있고, 테스트 주석에 이유가 적혀 있다 — **enum** (경로 권한에
`Role` 을 쓰는 것까지 막으면 같은 상수를 `global` 에 복제해야 한다)과 **auth 연결**
(`SecurityConfig` 는 JWT 필터와 핸들러를 엮어야 한다).

---

## 함께 쓰는 파일 — 바꾸기 전에 물어본다

아래 파일은 전 도메인이 공유한다. 혼자 고치면 남의 작업을 깨뜨린다.
**필요한 변경이 있으면 이슈로 요청한다.**

| 파일 | 왜 공유인가 |
|---|---|
| `global/config/SecurityConfig.java` | 모든 경로의 권한이 여기 한 곳에 있다 |
| `src/main/resources/application*.yml` | 전 환경의 설정 |
| `global/dto/*` · `global/exception/GlobalExceptionHandler.java` | 모든 응답의 모양 |
| `User` · `Generation` 엔티티 | 거의 모든 도메인이 FK 로 참조한다 |

지금 누가 어느 영역을 보는지는 저장소에 적어두지 않는다 — 사람이 바뀌면 문서가 낡는다.
`git log --oneline -- <경로>` 로 최근에 그 파일을 만진 사람을 찾거나, 팀 채널에 묻는다.
