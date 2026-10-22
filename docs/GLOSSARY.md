# 용어 사전

이 프로젝트에서만 통하는 어휘를 모았다. 코드를 읽다 모르는 말이 나오면 여기로 돌아온다.

정의는 전부 코드에서 가져왔고, 각 항목에 **정의가 있는 파일**을 달았다.
문서와 코드가 다르면 코드가 맞다.

---

## 세 영역

하나의 API 서버가 성격이 다른 세 영역을 담당한다. 경로 prefix 로 갈린다.

| 영역 | prefix | 누가 쓰나 | 인증 |
|---|---|---|---|
| 공개 사이트 | `/api/public/**` | 누구나 (비로그인 포함) | 없음 |
| 부원 LMS | `/api/member/**` | 부원 · 운영진 | `MEMBER` 이상 |
| 운영진 어드민 | `/api/admin/**` | 운영진 | `ADMIN` |

`/api/applications/**` 와 `/api/files/**` 는 셋 중 어디도 아니다 — **로그인만 했으면 되고
부원이 아니어도 된다.** 지원자는 아직 부원이 아니기 때문이다.

---

## 기수 · 모집

### 기수 (Generation)

`setting/generation/entity/Generation.java`

동아리의 한 기(예: 9기). **지원서 · 강의 · 조 등 거의 모든 도메인이 FK 로 참조한다.**
어떤 데이터를 보든 "몇 기 것인가"가 따라붙는다고 생각하면 된다.

- `generationNo` — 기수 번호 (9)
- `generationYear` — 연도 (2026). `year` 는 SQL 예약어라 컬럼명을 분리했다. **JSON 키는 `year` 그대로다**
- `isActive` — 지금 진행 중인 기수인지

### 활성 기수 (active generation)

`isActive = true` 인 기수. **항상 정확히 하나다.** 새 기수를 활성화하면 기존 기수를
비활성화하는 일이 한 트랜잭션에서 일어난다.

> ⚠️ **0번 기수는 진짜 기수가 아니다.**
> `Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO = 0` 은 활성화 로직을 직렬화하기 위한
> **잠금 행 전용** 예약 번호다. 기수 번호로는 절대 쓰이지 않는다.
> 기수를 조회하는 코드는 이 값을 걸러내야 한다 (`GenerationQueryService.findByGenerationNo` 가 그렇게 한다).

### 활성 기수 스코프 (active generation scope)

**이 저장소에서 가장 중요한 규칙 하나.** 기수에 속한 데이터를 수정·삭제할 때는
그게 활성 기수 소속인지 반드시 확인한다. `findById` 만 쓰면 ID 만 아는 사람이
지난 기수 데이터를 고칠 수 있다.

서비스에서 조회한 뒤 비교하지 말고, **저장소 메서드로 스코프한다** — 빠뜨릴 여지가 없다.

```java
Optional<EvaluationCriterion> findByIdAndGenerationId(Long id, Long generationId);
```

자세한 내용은 [PITFALLS.md](PITFALLS.md) 참조.

### 모집 일정 (RecruitmentSchedule)

`recruitment/entity/RecruitmentSchedule.java`

기수별 모집 일정. 기수와 1:1 이지만 **`Generation` 이 갖지 않고 분리돼 있다.**
`totalStartAt` · `documentStartAt` · `interviewStartAt` 등을 가진다.

`applyEnabled` 로 운영진이 지원 접수를 **수동으로 열고 닫을 수 있다.**

### 모집 단계 (RecruitmentPhase)

`recruitment/entity/RecruitmentPhase.java`

현재 시각과 모집 일정을 비교해 **계산되는** 값이다. DB 에 저장하지 않는다.

| 값 | 뜻 |
|---|---|
| `BEFORE_OPEN` | 모집 시작 전 |
| `DOCUMENT_OPEN` | 서류 접수 중 — 이때만 지원서를 낼 수 있다 |
| `DOCUMENT_REVIEW` | 서류 심사 중 |
| `INTERVIEW` | 면접 기간 |
| `FINAL_ANNOUNCED` | 최종 발표 후 |
| `CLOSED` | **활성 기수가 없을 때** |

`CLOSED` 만 성격이 다르다. 나머지는 일정만 보면 판정되는데 `CLOSED` 는 기수 자체가
없는 상태라 `resolvePhase` 밖에서 판단한다.

---

## 지원 · 평가

### 지원서 (Application)

`recruitment/entity/Application.java` · 상태는 `ApplicationStatus.java`

**1인 1기수 1지원.** 한 사람이 같은 기수에 두 번 낼 수 없다.

| 값 | 화면 표기 |
|---|---|
| `DRAFT` | 임시 저장 |
| `SUBMITTED` | 심사 중 |
| `DOC_PASS` / `DOC_FAIL` | 서류 합격 / 서류 불합격 |
| `FINAL_PASS` / `FINAL_FAIL` | 최종 합격 / 최종 불합격 |

한글 라벨은 **enum 의 `label()` 에만 둔다.** 결과 조회와 엑셀 다운로드가 같은 매핑을
필요로 해서, 두 곳에 복사하는 대신 enum 하나로 모았다.

### 지원서 질문 (ApplicationQuestion) · 답변 (ApplicationAnswer)

운영진이 기수마다 질문 항목을 직접 구성한다. 주관식 · 객관식 · 체크박스.

### 평가 기준 (EvaluationCriterion) · 평가 점수 (EvaluationScore)

한 지원서를 **여러 운영진이 평가하고 점수를 합산**한다.

배점 합계 규칙을 조심해서 읽어야 한다. **저장할 때는 "100 초과 금지"만 검증한다.**
정확히 100인지는 거부 사유가 아니라 `EvaluationCriteriaSummary.valid()` 로 안내만 한다.

> 명세서 문구는 "모든 쓰기 시점에 합계 100 검증" 이었는데, 그대로 구현하면 기준을 하나씩
> 추가하는 중간 상태(20점만 넣은 시점)조차 저장할 수 없다. 그래서 구현이 명세서와 다르게
> 갔다. 근거는 `EvaluationCriterionService.validateTotal` 의 주석에 적혀 있다.

"미평가" 는 별도 상태값이 아니라 **해당 지원서에 평가 레코드가 없는 것**이다.

---

## 강의 · 과제

### 트랙 (Track) · 소분류 (SubCategory)

`setting/category/entity/Track.java` · `SubCategory.java`

강의 분류 2단계. 트랙이 대분류(SW · 창업 · 세미나), 소분류가 그 아래(React.js · 린캔버스 …).
운영진이 직접 추가·삭제할 수 있다.

> **기수와 무관하다.** 둘 다 `generationId` 를 갖지 않는 전역 데이터다.
> 기수가 바뀌어도 분류는 그대로 쓴다.

`order` 는 SQL 예약어라 컬럼명이 **`display_order`** 다. 이 프로젝트에서 순서를 가진
리소스는 모두 같은 규칙을 쓴다.

### 강의 (Lecture) · 과제 (Assignment)

강의는 기수 · 트랙 · 소분류 · 주차(`week`)에 속한다.
**강의당 과제는 하나다** — `assignment.lecture_id` 에 unique 제약이 걸려 있어
코드가 실수해도 DB 가 막는다.
강의 설명과 과제 설명은 **마크다운 원문으로 저장**하고 렌더링은 프론트가 한다.

### 제출 (AssignmentSubmission)

`lecture/entity/AssignmentSubmission.java`

- 제출 방식(`SubmissionType`) — `FILE` 또는 `LINK`. 과제별로 허용 방식을 지정한다
- 상태(`SubmissionStatus`) — `SUBMITTED` 또는 `LATE`(마감 후 제출)
- **재제출은 덮어쓰기**다. 이력을 쌓지 않는다
- 링크 제출은 **호스트 화이트리스트**를 통과해야 한다 (`AllowedLinkHost`).
  아무 URL 이나 허용하면 운영진이 피드백 중 낚시 링크를 누를 수 있다

### 피드백 (Feedback)

운영진이 제출물에 남기는 글. 운영진 화면은 제출물을 **순차 탐색**(이전 / 저장 후 다음)
하므로 커서 기반 조회를 쓴다.

---

## 사용자 · 조

### 권한 (Role)

`user/entity/Role.java`

| 값 | 화면 표기 | 할 수 있는 것 |
|---|---|---|
| `GUEST` | 비회원 | 공개 페이지 + 지원서 작성·제출 |
| `MEMBER` | 부원 | + 강좌 · 과제 제출 · Q&A · 내 정보 |
| `ADMIN` | 운영진 | + 전체 어드민 |

최초 Google 로그인 시 `GUEST` 로 생성된다. 합격하면 운영진이 `MEMBER` 로 승격한다.

Spring Security 의 `hasRole()` 은 `ROLE_` 접두사를 요구하므로 `getAuthority()` 가
`ROLE_MEMBER` 형태로 만들어 준다.

### 사용자 상태 (UserStatus)

`ACTIVE`(활동) · `WITHDRAWN`(탈퇴). 탈퇴는 **soft delete** 다 — 행을 지우지 않는다.

탈퇴한 사용자가 다시 로그인하면 계정을 되살리되 **권한은 `GUEST` 로 되돌린다.**
배경은 [adr/adr_001.md](adr/adr_001.md) 참조.

### 조 (Group)

`user/entity/Group.java`

기수 안에서 부원을 나눈 묶음(1조 ~ N조). 테이블명은 **`user_group`** — `group` 이
SQL 예약어이기 때문이다.

> ⚠️ **조인 테이블이 없다.** `GroupMember` 같은 테이블 없이 `users.group_id` 한 컬럼으로
> 연결된다. 즉 **한 부원은 한 조에만 속한다.**
> 조원 추가·제거는 `User.assignToGroup(groupId)` 와 해제로 동작한다.

### 운영진 프로필 (Staff) · 구분 (StaffSection)

`setting/staff/entity/Staff.java` · `StaffSection.java`

공개 "운영진 소개" 페이지에 나가는 프로필. `Role.ADMIN` 과는 별개다 —
권한이 아니라 **소개 콘텐츠**다.

`EXECUTIVE`(회장단) · `SW`(SW 운영진) · `STARTUP`(창업 운영진) 으로 묶어 보여준다.

---

## 그 밖의 도메인

### Q&A (Question · Answer)

`qna/entity/Question.java`

`lectureId` 가 **nullable** 이다. 값이 있으면 강의 Q&A, 없으면 사이트 전체 Q&A —
한 테이블로 두 쓰임을 처리한다.

상태(`QnaStatus`)는 `PENDING`(답변 대기) · `ANSWERED`(답변 완료).
질문은 **작성자만 본다.**

### 프로젝트 (Project) · 상태 (ProjectStatus)

`project/entity/Project.java` · `ProjectStatus.java`

| 값 | 화면 표기 |
|---|---|
| `PENDING` | 승인 대기 |
| `APPROVED` | 공개 |
| `REJECTED` | 반려 |

**부원이 올리면 `PENDING`, 운영진이 승인해야 공개 쇼케이스에 나온다.**
공개 홈에 바로 노출되는 자리라 사람이 한 번 보고 넘긴다.
운영진이 직접 등록한 것은 처음부터 `APPROVED` 다.

### 행사 (Event · EventType)

`COMPETITION` · `WORKSHOP` · `EVENT`. 공개 홈의 월별 캘린더와 어드민 D-day 양쪽에서 쓴다.

### 기능 토글 (FeatureToggle · FeatureKey)

`STOCK_GAME`(모의 주식 게임) · `MOCK_INVESTMENT`(모의 투자).
**기능 자체는 아직 구현되지 않았고 on/off 스위치만 있다.**

> enum 에 값을 추가하면 **시드 데이터도 넣어야 한다.**

---

## 파일

### 파일 용도 (FilePurpose) · 공개 범위 (StorageVisibility)

`file/entity/FilePurpose.java` · `StorageVisibility.java`

용도마다 허용 확장자 · 최대 용량 · **어느 저장소에 둘지**가 정해져 있다.

| 용도 | 최대 | 공개 범위 |
|---|---|---|
| `LECTURE_MATERIAL` | 50MB | `PRIVATE` |
| `ASSIGNMENT` | 50MB | `PRIVATE` |
| `PROFILE_IMAGE` | 5MB | `PUBLIC` |
| `PROJECT_THUMBNAIL` | 5MB | `PUBLIC` |
| `ACTIVITY_PHOTO` | 5MB | `PUBLIC` |

- `PUBLIC` — 누구나 URL 로 읽는다. 주소가 고정이라 브라우저 캐시가 걸린다
- `PRIVATE` — 권한을 확인한 뒤 **짧게 사는 서명 주소**로만 읽는다

공개 이미지에까지 매번 서명 주소를 발급하면 URL 이 계속 바뀌어 캐시가 안 걸린다.
그래서 둘을 나눴다. 저장 키 앞에 `public/` · `private/` 접두어가 붙어
**키만 보고 어느 저장소인지 알 수 있다.**

### 파일 상태 (FileStatus)

`PENDING` — 업로드됐지만 아직 어느 도메인에도 연결되지 않은 파일
`CONNECTED` — 연결된 파일

---

## 코드를 읽을 때 자주 보이는 말

| 말 | 뜻 |
|---|---|
| **envelope** | 모든 응답을 감싸는 `ApiResponse<T>`. `{ success, data, error }` 세 칸 |
| **soft delete** | 행을 지우지 않고 `deletedAt` 에 시각을 적는 삭제. 조회할 때 `deletedAt is null` 을 빠뜨리면 지운 게 다시 나온다 |
| **크로스 도메인 계약** | 다른 도메인의 데이터를 읽어야 할 때 쓰는 조회 인터페이스. Repository 를 직접 주입받지 않는다 ([ARCHITECTURE.md](ARCHITECTURE.md)) |
| **Result DTO** | 서비스가 반환하는 전용 타입. 엔티티를 컨트롤러로 내보내지 않는다 |
| **Command record** | 파라미터가 많을 때 묶는 record. 같은 타입이 인접하면 순서를 바꿔도 컴파일이 통과해서 위험하다 |
| **tie-breaker** | 정렬의 마지막 기준(보통 `id`). 없으면 페이징에서 같은 행이 두 번 나오거나 누락된다 |
| **예약 표** | 마이그레이션 번호를 미리 찜하는 표. [db/migration/README.md](../src/main/resources/db/migration/README.md) |
