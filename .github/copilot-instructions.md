# GETIT 백엔드 — Copilot 지침

경북대 창업 IT 동아리 **GETIT** 통합 운영 플랫폼의 백엔드입니다.
이 문서는 `GETIT_Site_BE` 레포지토리 전체에 적용됩니다.

응답·주석·커밋 메시지는 **한국어**로 작성합니다. 코드 식별자는 영어입니다.

---

## 기술 스택

| | |
|---|---|
| 언어 · 런타임 | Java 21 (Gradle toolchain) |
| 프레임워크 | Spring Boot 3.5.16 |
| 영속성 | Spring Data JPA · MySQL 8.4 (테스트는 H2) |
| 마이그레이션 | Flyway |
| 인증 | Spring Security + OAuth2 Client (Google) · JJWT 0.13.0 |
| 문서 | SpringDoc OpenAPI 3 |
| 빌드 | Gradle 9.x · Spotless(googleJavaFormat) |

`api/v1` 같은 버전 prefix는 쓰지 않습니다. 경로는 `/api/...` 로 시작합니다.

---

## 패키지 구조 — 도메인 수직 분할

```
com.getit
├── global/                 공통. ApiResponse · 예외 · 설정 · BaseTimeEntity
└── domain/
    ├── auth/               인증 · JWT · Security
    ├── user/               User · College · Major
    ├── recruitment/        모집 일정 · 지원서 질문 · 평가 기준 · 지원서
    ├── lecture/            강의 · 과제 · 제출
    ├── file/               파일 업로드 · 연결
    └── setting/
        ├── generation/     기수
        └── category/       트랙 · 소분류
```

각 도메인은 `controller / service / repository / entity / dto / exception` 을 갖습니다.
관리자 전용이 커지면 `admin/` 하위 패키지로 분리합니다 (`lecture/admin/...`).

**도메인 경계를 넘지 마십시오.** 다른 도메인의 `Repository` 를 직접 주입하는 것은 금지입니다.

---

## 크로스 도메인 규칙

다른 도메인의 데이터가 필요하면 **서비스 인터페이스 계약**을 거칩니다.

```java
// ❌ 금지
private final UserRepository userRepository;

// ✅ 인터페이스 경유
private final UserAccountService userAccountService;
private final GenerationQueryService generationQueryService;
private final CategoryQueryService categoryQueryService;
```

**계약을 어디에 정의할지는 방향으로 정합니다.**

| 상황 | 정의 위치 | 예 |
|---|---|---|
| 제공자가 소비자를 이미 알아도 됨 | **제공자** 도메인 | `UserQueryService` (user) |
| 제공자가 소비자를 몰라야 함 (DIP) | **소비자** 도메인 | `CategoryLectureLinkService` (lecture) |

**인터페이스는 소비자가 붙기 전에 완성합니다.** 나중에 시그니처를 바꾸면 양쪽 PR을 동시에
건드려야 합니다. 특히 **목록을 다루는 소비자가 있으면 배치 조회 메서드를 처음부터 넣으십시오**
(`countBySubCategoryIds` 처럼). 단건 메서드만 두면 소비자 쪽에서 N+1이 납니다.

---

## 응답 · 에러 규약

모든 응답은 `ApiResponse` 로 감쌉니다.

```java
public record ApiResponse<T>(boolean success, T data, ErrorResponse error) { }
public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) { }
```

```java
return ApiResponse.success(result);   // 200 / 201
```

### 에러 코드는 도메인 enum에 정의합니다

```java
public enum RecruitmentErrorCode implements ErrorCode {
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문 항목을 찾을 수 없습니다."),
    INVALID_CRITERIA_TOTAL(HttpStatus.BAD_REQUEST, "평가 기준 배점 합계는 100점을 초과할 수 없습니다.");
}
```

- **`CommonErrorCode.VALIDATION_FAILED` 는 `@Valid` 실패 전용입니다.**
  비즈니스 규칙 위반에 재사용하지 마십시오 — 프론트가 `code` 로 분기할 수 없게 됩니다.
- **같은 `code` 에 서로 다른 `message` 를 쓰지 마십시오.**
- `message` 는 fallback 입니다. 프론트가 분기 근거로 쓰는 것은 `code` 뿐입니다.
- 도메인마다 `@RestControllerAdvice` 를 둘 때는 **반드시 `@Order` 를 붙입니다.**
  Spring은 먼저 매칭된 advice에서 반환하며, 구체성 우선순위는 하나의 advice 안에서만 적용됩니다.

---

## 엔티티

```java
@Entity
@Table(name = "application_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationQuestion extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder(access = AccessLevel.PRIVATE)
    private ApplicationQuestion(...) { ... }

    public static ApplicationQuestion create(...) { return builder()...build(); }
}
```

- **JPA 연관관계(`@ManyToOne` 등)를 쓰지 않습니다.** 외래키는 `Long generationId` 처럼
  plain 컬럼으로 둡니다. 이 프로젝트 전체의 관례입니다.
- 정적 팩토리(`create` · `createDraft`)로 생성하고, 생성자는 `private` + `@Builder` 입니다.
- **검증은 서비스 레이어에서 합니다.** 엔티티는 받은 값을 그대로 담습니다.
- enum 필드는 `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)`,
  컬럼은 `varchar` 입니다. 네이티브 ENUM 도 CHECK 제약도 쓰지 않습니다.
- JSON 컬럼은 `AttributeConverter` 대신 **`@JdbcTypeCode(SqlTypes.JSON)`** 을 우선 검토합니다
  (Spring이 설정한 `ObjectMapper` 가 쓰입니다).
- `order` 는 SQL 예약어입니다. 컬럼명을 `question_order` 처럼 분리하고 이유를 주석에 남깁니다.

### 원시 타입 규칙

`nullable = false` 인 필드는 **원시 타입**(`int` · `boolean` · `long`)을 씁니다.
박싱 타입에 `null` 이 들어가면 `IS NULL` 조회가 되어 조용히 틀립니다.

**단 요청 DTO는 예외입니다.** `Integer` 를 유지해야 `@NotNull` 이 "누락"을 감지합니다.
`int` 로 바꾸면 필드가 없을 때 0으로 역직렬화되어 검증을 통과합니다.

---

## 서비스

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)      // 클래스 레벨
public class ApplicationQuestionService {

    @Transactional                    // 쓰기 메서드에만
    public Result createQuestion(...) { ... }
}
```

- 서비스 인터페이스를 따로 만들지 않습니다. 구현 클래스 하나입니다.
  (크로스 도메인 계약은 예외 — 위 참조)
- **`@Transactional` 은 프록시 기반입니다.** 같은 클래스 내부 호출(self-invocation)에는
  적용되지 않습니다.
- **서비스는 Result DTO를 반환합니다. 엔티티를 컨트롤러로 내보내지 마십시오.**
  지연로딩 연관이 붙는 순간 `LazyInitializationException` 이 납니다.
- **파라미터가 3개를 넘거나 같은 타입이 인접하면 Command record 로 묶습니다.**
  `create(String name, String guideline, int score)` 처럼 `String` 두 개가 붙어 있으면
  순서를 바꿔도 컴파일이 통과합니다.

### 활성 기수 스코프 — 매우 중요

기수에 속한 리소스를 수정·삭제할 때는 **활성 기수 소속인지 반드시 확인합니다.**
`findById` 만 쓰면 ID만 아는 사람이 지난 기수 데이터를 고칠 수 있습니다.

```java
// ✅ 저장소에서 한 번에
Optional<EvaluationCriterion> findByIdAndGenerationId(Long id, Long generationId);
```

서비스에서 조회 후 비교하는 방식보다 **저장소 메서드로 스코프하는 쪽을 씁니다.**
빠뜨릴 여지가 없습니다.

### 순서(order) 관리

`order` 컬럼을 쓰는 리소스는 **1..n 연속**을 불변식으로 유지합니다.

- 새 항목의 order 는 `count + 1` 이 아니라, **삭제 시 뒤 항목을 당긴 뒤**의 개수 기준입니다.
- 삭제 후 재정렬을 하지 않으면 order 중복이 생기고, 정렬 결과가 비결정적이 됩니다.
- 순서 변경 API는 **활성 기수의 전체 집합과 정확히 일치**하는지 검증합니다
  (부분 목록 · 중복 ID · 타 기수 ID를 한 번에 막습니다).

---

## Repository · 쿼리

- 파생 쿼리 이름이 `order` 같은 필드명과 겹쳐 헷갈리면 `@Query` 로 명시합니다.
- **정렬에는 항상 tie-breaker 를 넣습니다** — `order by l.week asc, l.id asc`.
  특히 페이징에서 정렬이 불안정하면 같은 행이 두 번 나오거나 누락됩니다.
- 목록을 조회한 뒤 각 항목마다 다시 조회하지 마십시오. **`findAllBy...In` + `toMap`** 을 씁니다.
- soft delete 를 쓰는 엔티티는 조회 시 `deletedAt is null` 을 빠뜨리지 마십시오.

---

## DB 마이그레이션 — 사고가 가장 많이 나는 곳

### 번호는 이슈에서 선점합니다

`db/migration/README.md` 예약 표에 등록한 뒤 사용합니다.
**PR 리뷰에서 번호를 정하지 않습니다** — 여러 PR이 동시에 열리면 반드시 충돌합니다.

- 이미 적용된 번호보다 **낮은 번호를 나중에 추가하면 Flyway 검증이 실패**합니다
  (`Detected resolved migration not applied to database`). `outOfOrder` 는 켜지 않습니다.
- 결번은 무해합니다. 되살리려 하지 마십시오.
- **CI 스키마 검증은 매번 빈 MySQL 에서 시작하므로 이 문제를 잡지 못합니다.**

### 새 테이블에는 인덱스를 함께 넣습니다

**이 프로젝트에서 가장 자주 누락되는 항목입니다.** 조회 조건과 정렬 키를 보고 잡으십시오.

```sql
KEY idx_lecture_generation_week (generation_id, week, id)
```

유니크 제약이 조회 패턴의 선행 컬럼을 덮으면 별도 인덱스가 필요 없습니다
(`uk_application_answer_question (application_id, question_id)` 는 `findByApplicationId` 를 커버).

### 그 외

- 로컬(`ddl-auto: update`)에서 Hibernate가 생성한 DDL을 옮기되, **enum CHECK 제약은 제거**합니다.
- 시드 데이터에서 **`id` 를 하드코딩하지 마십시오.** `AUTO_INCREMENT` 시작값은 서버 설정에
  따라 달라집니다. `SELECT c.id FROM college c WHERE c.name = '...'` 로 조회해서 넣습니다.
- 스키마 변경과 데이터 적재는 **다른 마이그레이션 번호**로 분리합니다.

---

## DTO · 검증

```java
public record ApplicationQuestionRequest(
    @NotNull QuestionType type,
    @NotBlank @Size(max = 500) String content,      // 엔티티 @Column(length = 500) 과 일치
    @Positive @Max(2000) Integer maxLength
) { }
```

- **DTO의 `@Size` 상한은 엔티티 `@Column(length)` 와 반드시 일치시킵니다.**
  없으면 초과 입력이 검증을 통과해 INSERT까지 가고 `DataIntegrityViolationException` →
  **400이어야 할 응답이 500이 됩니다.**
- 숫자 필드에 범위 검증(`@Positive` · `@Min` · `@Max`)을 빠뜨리지 마십시오.
- 요청 DTO와 응답 DTO를 공유하지 않습니다. `...Request` / `...Result` 로 나눕니다.
- 중첩 DTO를 검증하려면 필드에 `@Valid` 가 필요합니다.

---

## 시간 처리

- 엔티티는 `LocalDateTime` 을 씁니다.
- **응답 DTO는 `OffsetDateTime`** 입니다 (명세서가 `+09:00` 오프셋을 명시합니다).
- **`LocalDateTime.now()` 를 서비스에서 직접 호출하지 마십시오.** `Clock` 빈을 주입합니다.
  시간에 따라 결과가 갈리는 로직(마감 검증 등)은 `Clock` 없이는 경계값을 테스트할 수 없습니다.

---

## 테스트

- 컨트롤러: `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional`
- 서비스: `@SpringBootTest` + `@Transactional`
- 저장소: `@DataJpaTest` + `@Import(JpaAuditingConfig.class)`
- `@Nested` + `@DisplayName` 으로 API 단위 그룹화, DisplayName은 한국어 서술문입니다.

반드시 포함할 것:

- **인가 테스트** — 토큰 없으면 401, 권한 없으면 403
- **활성 기수 아닌 리소스 접근 시 404**
- 경계값 (시작·종료 시각을 양쪽 다)
- **`AttributeConverter` · JSON 컬럼은 DB 왕복 테스트.**
  `flush()` + `entityManager.clear()` 를 하지 않으면 1차 캐시에서 값이 나와
  **읽기 경로가 한 번도 실행되지 않습니다.**
- 예외를 검증할 때는 롤백까지 확인합니다 (`실패한 요청은 저장되지 않아야 한다`)

인가 테스트는 상태 코드 하나에 고정하지 말고 **401/403 여부**로 검증합니다.

---

## 컨벤션

| | |
|---|---|
| 브랜치 | `feat/이슈번호-작업내용` — **`#` 를 붙이지 않습니다** |
| 커밋 | `feat(recruitment): ...` — 타입(스코프) 형식 |
| PR 크기 | **프로덕션 코드 500줄 이하** (테스트는 세지 않습니다) |
| 줄 길이 | 120자 |
| 포맷 | `./gradlew spotlessApply` |

PR이 500줄을 넘으면 쪼갭니다. 자연스러운 분할선:
**① 엔티티 + 마이그레이션 → ② 서비스 + 컨트롤러**.
**다른 PR의 후속 작업이 섞여 있으면 그것부터 떼어내십시오.**

---

## 코드 리뷰 체크리스트

Copilot이 이 레포의 PR을 리뷰할 때 아래를 우선 확인합니다.
**전부 실제로 반복해서 발생한 문제입니다.**

- [ ] 새 테이블의 조회 조건·정렬 키에 **인덱스**가 있는가 (누락 4회)
- [ ] 마이그레이션 번호가 **예약 표**에 등록됐고 기존 최대 번호보다 큰가 (충돌 4회)
- [ ] 기수에 속한 리소스의 수정·삭제에 **활성 기수 스코프**가 걸렸는가
- [ ] `order` 를 쓰는 리소스에서 **삭제 후 재정렬**을 하는가
- [ ] DTO `@Size` 가 엔티티 `@Column(length)` 와 **일치**하는가 (불일치 시 500 발생)
- [ ] 비즈니스 검증 실패에 **도메인 `ErrorCode`** 를 썼는가 (`VALIDATION_FAILED` 재사용 금지)
- [ ] `nullable = false` 필드가 **원시 타입**인가 (요청 DTO는 예외)
- [ ] 목록 조회에 **N+1** 이 없는가 (루프 안의 `findById` · `save`)
- [ ] 정렬에 **tie-breaker** 가 있는가
- [ ] 서비스가 **엔티티가 아닌 Result DTO** 를 반환하는가
- [ ] `Map.get()` 결과를 **null 검사 없이** 쓰는 곳이 없는가
- [ ] 다른 도메인의 **Repository 직접 주입**이 없는가
- [ ] `LocalDateTime.now()` 직접 호출 대신 **`Clock`** 을 주입했는가
- [ ] 브랜치명에 **`#`** 이 없는가
- [ ] 프로덕션 코드가 **500줄** 이하인가

---

## 명세서

`GETIT-API-명세서.md` 가 단일 기준입니다 (13개 도메인 · 104개 엔드포인트).
컨트롤러·서비스 주석에 해당 번호를 남깁니다 — `(API 명세서 6.3 · 6.4)`.

**명세서와 구현이 다르면 구현을 바꾸기 전에 명세서 결함인지 먼저 확인하십시오.**
명세서에 없는 필드를 임의로 추가하거나, 있는 필드를 임의로 빼지 않습니다.
판단이 필요하면 PR 본문의 「리뷰 포인트」에 적고 넘깁니다.
