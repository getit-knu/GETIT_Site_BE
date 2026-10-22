# 처음 투입됐다면

이 저장소는 **이미 다 만들어져서 돌아가고 있는 서비스**다. 빈 프로젝트에 기능을 더하는 게
아니라, 남이 쓴 코드 사이에 끼어들어야 한다. 그래서 "무엇을 알아야 하나" 보다
**"무엇을 순서대로 하면 되나"** 로 썼다.

위에서 아래로 따라가면 된다. **반나절 ~ 하루** 분량이다.

| | |
|---|---|
| **가정하는 것** | Java 문법 · Spring Boot 로 API 를 만들어 본 경험 · JPA 로 엔티티를 다뤄 본 경험 · Git 브랜치와 PR |
| **가정하지 않는 것** | 이 프로젝트의 도메인 · 구조 · 규칙. 전부 아래에서 설명한다 |

Spring 이나 JPA 자체가 처음이라면 먼저 공식 가이드로 작은 CRUD 를 한 번 만들어 보는 쪽이
빠르다. 이 문서는 **이 프로젝트 고유의 것**만 다룬다.

---

## 0. 이게 뭐 하는 서버인가

GETIT 동아리 사이트의 백엔드다. **하나의 서버가 성격이 다른 세 영역을 담당한다.**

| 영역 | 누가 쓰나 | 예 |
|---|---|---|
| 공개 사이트 | 누구나 | 홈 · 모집 일정 · 운영진 소개 · 프로젝트 쇼케이스 |
| 부원 LMS | 동아리 부원 | 강의 열람 · 과제 제출 · Q&A |
| 운영진 어드민 | 운영진 | 지원자 평가 · 강의 관리 · 사용자 관리 · 사이트 설정 |

1년이 **기수**(期) 단위로 돌아간다. 모집 → 지원서 심사 → 합격자를 부원으로 승격 →
강의와 과제 → 프로젝트. **거의 모든 데이터에 "몇 기 것인가" 가 붙는다.**
이게 이 도메인의 가장 중요한 성질이다.

규모는 이 정도다 (세는 명령을 같이 적어둔다 — 숫자는 계속 늘어난다).

```bash
grep -rhoE '@(Get|Post|Put|Patch|Delete)Mapping' --include='*Controller.java' src/main/java | wc -l   # 엔드포인트
find src/main/java -path '*entity*' -name '*.java' | wc -l                                            # 엔티티
ls src/main/resources/db/migration/V*.sql | wc -l                                                     # 마이그레이션
find src/test -name '*Test.java' | wc -l                                                              # 테스트 파일
```

**엔드포인트 목록은 문서로 관리하지 않는다.** 앱을 띄우고
<http://localhost:8080/swagger-ui.html> 를 본다. 손으로 쓴 목록은 반드시 낡는다.

---

## 1. 띄운다

[README.md](../README.md) 의 "시작하기" 를 따라간다. `.env` 를 만들고 `docker compose up -d`
후 `./gradlew bootRun` 이면 끝이다.

처음 막히는 지점은 거의 항상 `.env` 다. 두 가지만 기억한다.

- **`.env` 가 없으면 기동이 즉시 실패한다** (`Config data resource 'file [.env]' ... does not exist`)
- **주석을 풀고 값을 비워두면 기동이 실패한다.** 빈 문자열도 '값' 으로 취급된다. 채울 때만 주석을 해제한다

Google 로그인은 안 되는 게 정상이다. `GOOGLE_CLIENT_ID` 가 없어도 **기동 · Swagger ·
공개 API · 파일 업로드는 다 된다.** 로그인 시도할 때만 실패한다.

### 떴는지 확인

```bash
curl localhost:8080/api/public/recruitment/status
```

`{"success":true,"data":{...},"error":null}` 모양이 나오면 성공이다.
이 세 칸짜리 껍데기를 **envelope** 이라고 부른다. 모든 응답이 이 모양이다.

```bash
./gradlew test     # 테스트도 돌려본다. H2 를 쓰므로 docker 없이도 된다
```

---

## 2. 말을 익힌다

[GLOSSARY.md](GLOSSARY.md) 를 한 번 훑는다. **외우지 않아도 된다** — 코드를 읽다 모르는
말이 나올 때 돌아올 자리를 알아두는 게 목적이다.

지금 머리에 넣어둘 것은 네 개뿐이다.

| 말 | 뜻 |
|---|---|
| **기수** | 동아리의 한 기(9기). 거의 모든 데이터가 이걸 FK 로 참조한다 |
| **활성 기수** | 지금 진행 중인 기수. **항상 하나다** |
| **envelope** | 모든 응답을 감싸는 `{ success, data, error }` |
| **트랙 / 소분류** | 강의 분류 2단계 (SW → React.js) |

---

## 3. 워크스루 ① 읽기 경로를 끝까지 따라간다

방금 `curl` 로 때려본 그 API 다. **프로덕션 코드 240줄 정도로 이 프로젝트의 핵심 규칙이
거의 다 들어 있다.** 아래 순서로 직접 열어서 읽는다.

> `GET /api/public/recruitment/status` — 모집 상태와 D-day

### ① 컨트롤러 — `recruitment/controller/RecruitmentStatusController.java`

26줄이다. **컨트롤러는 이만큼 얇다.**

```java
@GetMapping
public ApiResponse<RecruitmentStatusResult> getStatus() {
  return ApiResponse.success(recruitmentStatusService.getStatus());
}
```

- 하는 일이 **서비스 호출과 `ApiResponse` 로 감싸기** 뿐이다
- 비즈니스 로직 · 쿼리 · `try-catch` · `@Transactional` 이 **하나도 없다**
- `@Operation(description = "명세서 2.8")` — 주석에 API 명세 번호를 남기는 관례가 보인다

### ② 서비스 — `recruitment/service/RecruitmentStatusService.java`

117줄. **여기가 두껍다.** 클래스 주석을 꼭 읽을 것 — 왜 이렇게 됐는지가 적혀 있다.

눈여겨볼 네 가지:

1. **`implements RecruitmentStatusQueryService`**
   다른 도메인이 모집 상태를 알아야 할 때 쓰는 **크로스 도메인 계약**이다.
   이 프로젝트에서 `interface` 를 두는 **유일한 경우**다 (26개 있다).

2. **`private final Clock clock;`**
   `LocalDateTime.now()` 를 직접 부르지 않는다. 테스트에서 단계 경계값을 고정하지 못하면
   **실행 시각에 따라 결과가 달라지고 자정이 끼면 간헐적으로 실패한다.**
   주석에 실제 리뷰 지적(PR #86)까지 적혀 있다.

3. **`generationQueryService.findActive()`**
   활성 기수를 직접 쿼리하지 않고 **계약을 거친다.** `setting/generation` 의
   Repository 를 주입받으면 안 된다 — [ArchUnit 테스트가 막는다](ARCHITECTURE.md#archunit-이-강제하는-것).

4. **활성 기수가 없으면 `CLOSED` 로 응답한다.**
   공개 API 라서 404 로 실패하지 않는다. 관리자 API 와 정책이 다르다.

`PAUSED_MESSAGE` 주석도 읽어볼 것. **새 상태값을 만들지 않고 기존 값으로 처리한 이유**가
적혀 있다 — 이 저장소가 주석을 어떻게 쓰는지 보여주는 좋은 예다.

### ③ 결과 타입 — `recruitment/dto/RecruitmentStatusResult.java`

서비스는 **엔티티를 반환하지 않는다.** 전용 Result DTO 로 바꿔 내보낸다.
엔티티를 그대로 내보내면 지연로딩 연관이 붙는 순간 `LazyInitializationException` 이 난다.

### ④ 상태 enum — `recruitment/entity/RecruitmentPhase.java`

6단계 상태 머신. **DB 에 저장하지 않고 계산되는 값**이다.
`CLOSED` 만 성격이 다른 이유가 주석에 있다.

### ⑤ 테스트 — `recruitment/service/RecruitmentStatusServiceTest.java`

252줄. **고정된 `Clock` 으로 교체해서 각 단계 경계값을 검증한다.**
`@Nested` + 한국어 `@DisplayName` 으로 묶는 방식도 여기서 익힌다.

```bash
./gradlew test --tests '*RecruitmentStatusServiceTest'
```

### 여기까지 보면 알게 되는 것

```
요청 → SecurityConfig(권한) → Controller(얇게) → Service(두껍게) → Repository
                                     ↑ ApiResponse 로 감싸기   ↓ Result DTO 로 변환
```

---

## 4. 워크스루 ② 쓰기 경로 — 규칙이 몰려 있는 곳

읽기는 쉽다. **쓰기가 어렵다.** 지원서 제출을 따라간다.

> `POST /api/applications/me/submit` — 지원서 제출

`recruitment/controller/ApplicationController.java`(83줄) → `service/ApplicationService.java` →
`repository/ApplicationRepository.java` → `entity/Application.java` →
마이그레이션 `V7` · `V34` → `service/ApplicationServiceTest.java`(635줄)

### 검증 순서가 코드 순서다

`ApplicationService.submit()` 을 보면 검증이 **정해진 순서로** 늘어서 있다.

1. 활성 기수가 있나 → 없으면 `APPLICATION_NOT_OPEN`
2. 지금이 모집 기간인가 (`findOpenSchedule`)
3. 개인정보 수집·이용에 동의했나
4. `Clock` 으로 현재 시각을 얻는다
5. 기존 지원서가 있으면 **`DRAFT` 인지 확인** — 이미 제출한 건 다시 못 낸다
6. 기본 정보 검증
7. **필수 질문에 다 답했나**
8. **답변 글자수 제한**
9. `application.submit(now, now)` — 상태를 `DRAFT` → `SUBMITTED` 로 전이

**명세서에 검증 순서가 정의돼 있으면 그 순서를 그대로 옮겨 적는다.**
구현 누락을 막기 위한 관례다.

### 엔티티에서 볼 것

`entity/Application.java` 를 열어 **세 가지**를 확인한다.

1. **`@ManyToOne` 이 없다.** FK 는 `Long userId` · `Long generationId` 같은 plain 컬럼이다.
   엔티티 51개 전부 그렇다
2. **`@Setter` 가 없다.** 상태 변경은 `submit()` 처럼 **의미가 드러나는 메서드**로 한다
3. **생성자가 `private`** 이고 정적 팩토리로만 만든다

`@Table` 의 unique 제약도 볼 것 — `uk_application_user_generation`.
**1인 1기수 1지원**이 DB 레벨에서 강제된다.

### 마이그레이션이 왜 따라붙는가

엔티티에 필드를 더하면 **반드시 마이그레이션이 같이 가야 한다.**
`V34__add_privacy_consent.sql` 을 열어본다. **20줄이 넘는 머리말 주석**이 있다 —
왜 이 컬럼이 필요한지, 왜 이 값으로 다른 기능을 막지 않는지까지 적혀 있다.

이 저장소가 주석을 쓰는 기준(**What 이 아니라 Why**)을 보여주는 가장 좋은 예다.

빠뜨리면 CI 의 **스키마 검증** job 이 잡는다.

```bash
docker compose up -d && ./gradlew schemaTest
```

---

## 5. 구조를 이해한다

여기까지 왔으면 [ARCHITECTURE.md](ARCHITECTURE.md) 가 읽힌다.

특히 **"왜 레이어가 아니라 도메인으로 나누는가"** 절을 읽을 것.
`controller/` · `service/` 를 최상위에 두지 않은 이유가 **협업 때문**이라는 게 핵심이다.

그다음 [CONVENTIONS.md](CONVENTIONS.md) 를 훑는다. 전부 외울 필요는 없다 —
**규칙에 "왜" 가 붙어 있으니 이유만 기억하면 응용이 된다.**

---

## 6. 첫 PR

작은 것부터 한다. 흐름은 [WORKFLOW.md](WORKFLOW.md) 에 자세히 있고, 여기선 순서만.

1. **이슈를 잡는다.** 이슈 없이 시작하지 않는다 — 브랜치명에 번호가 들어간다
2. **브랜치를 만든다.** `feat/203-privacy-consent`
   **`#` 을 붙이지 않는다** — 셸이 주석으로 해석한다
3. **스키마를 건드린다면 번호를 먼저 예약한다.**
   [db/migration/README.md](../src/main/resources/db/migration/README.md) 에 한 줄 추가.
   리뷰에서 번호를 정하면 **반드시 충돌한다** (실제로 4회 났다)
4. **테스트를 같이 쓴다.** 같은 PR 에 넣는다. **실패 경로를 반드시 넣는다**
5. **확인한다**

   ```bash
   ./gradlew spotlessApply && ./gradlew build
   ```

6. **[리뷰 체크리스트](PITFALLS.md#리뷰-체크리스트)로 자가 점검한다.** 15개 전부
   실제로 반복해서 터진 문제다
7. **PR 을 올린다.** 템플릿이 뜬다. **「리뷰 포인트」를 비워두지 않는다** —
   고민한 지점과 대안이 있었던 선택을 적는다
8. 교차 리뷰 **1 approve** 로 머지. 머지되면 CI → CD 로 자동 배포된다

### 프로덕션 코드 500줄 이하

테스트는 세지 않는다.

```bash
git diff --numstat main...HEAD -- src/main | awk '{s+=$1} END {print s" 줄"}'
```

넘으면 쪼갠다. 분할선은 **① 엔티티 + 마이그레이션 → ② 서비스 + 컨트롤러**.

---

## 막혔을 때

| 증상 | 볼 곳 |
|---|---|
| 기동이 안 된다 | [README.md](../README.md) 의 `.env` 절 |
| 어떤 API 가 있는지 모르겠다 | <http://localhost:8080/swagger-ui.html> |
| 코드에 모르는 말이 나온다 | [GLOSSARY.md](GLOSSARY.md) |
| 이걸 어디에 둬야 하나 | [ARCHITECTURE.md](ARCHITECTURE.md) |
| 이렇게 써도 되나 | [CONVENTIONS.md](CONVENTIONS.md) |
| 테스트가 깨진다 · 뭔가 조용히 틀렸다 | [PITFALLS.md](PITFALLS.md) |
| PR 을 어떻게 올리나 | [WORKFLOW.md](WORKFLOW.md) |
| 왜 이렇게 결정했나 | [adr/](adr/) · 그리고 **코드 주석** |
| 배포 · 운영 | [DEPLOYMENT.md](DEPLOYMENT.md) — 당장은 볼 필요 없다 |

**주석을 먼저 읽는 습관을 들일 것.** 이 저장소는 결정의 배경을 주석에 남긴다 —
`V34` 마이그레이션 머리말, `Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO`,
`RecruitmentStatusService` 클래스 주석이 그런 예다.
문서보다 주석이 정확할 때가 많다. **문서와 코드가 다르면 코드가 맞다.**

그래도 모르면 **묻는다.** 혼자 30분 넘게 막혀 있지 않는 쪽이 팀에 이득이다.
