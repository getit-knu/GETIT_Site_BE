# 코드 규약

**코드를 쓰기 전에** 읽는다. PR 을 올리기 전에는 [PITFALLS.md](PITFALLS.md) 를 훑는다.

여기 있는 규칙은 대부분 "취향" 이 아니라 **이미 사고가 났던 자리**다.
이유가 적혀 있는 규칙은 이유까지 읽어야 응용이 된다.

여섯 개는 [ArchUnit 테스트로 강제](ARCHITECTURE.md#archunit-이-강제하는-것)되므로
어기면 빌드가 깨진다. 나머지는 리뷰에서 걸린다.

---

## 엔티티

```java
@Entity
@Table(name = "application_question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplicationQuestion extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Builder(access = AccessLevel.PRIVATE)
  private ApplicationQuestion(...) { ... }

  public static ApplicationQuestion create(...) { return builder()...build(); }
}
```

### 연관관계 어노테이션을 쓰지 않는다

**`@ManyToOne` · `@OneToOne` 을 쓰지 않는다.** 외래키는 `Long generationId` 처럼
plain 컬럼으로 둔다. 같은 도메인 안에서도 마찬가지다.

현재 엔티티 51개 전부 이 규칙을 지키고 있다. 확인:

```bash
grep -rl '@ManyToOne\|@OneToOne' src/main/java | wc -l   # 0 이어야 한다
```

이 규칙이 [도메인 수직 분할](ARCHITECTURE.md#왜-레이어가-아니라-도메인으로-나누는가)의
전제다. 연관을 걸면 도메인 경계가 엔티티 그래프를 타고 무너진다.

### 생성은 정적 팩토리로만

`@AllArgsConstructor` 를 쓰지 않는다. **필드 순서가 바뀌면 조용히 깨지는 생성자**를
만들지 않기 위해서다. 세 단계로 막는다.

1. `@NoArgsConstructor(access = PROTECTED)` — JPA 용 기본 생성자만 열어둔다
2. `@Builder(access = PRIVATE)` 를 **생성자에** 붙인다 — 외부에서 빌더를 직접 못 쓴다
3. `create()` · `createDraft()` 같은 정적 팩토리를 유일한 생성 통로로 둔다 — 이름으로 의도가 드러난다

> `@Builder` 를 클래스에 붙이면 Lombok 이 all-args 생성자를 자동 생성한다.
> 그래서 클래스가 아니라 **생성자에** 붙인다.

### `@Setter` 를 쓰지 않는다

상태 변경은 **의미가 드러나는 메서드**로 정의한다.

```java
user.assignToGroup(groupId);      // ✅ 무슨 일이 일어나는지 읽힌다
user.setGroupId(groupId);         // ❌
```

### enum 필드

`@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.VARCHAR)`, 컬럼은 `varchar`.
**네이티브 ENUM 도 CHECK 제약도 쓰지 않는다.**

`ORDINAL` 이 기본값이라 깜빡하면 숫자로 저장되고, **enum 순서를 바꾸는 순간
기존 데이터가 전부 다른 값을 가리킨다.**

### 원시 타입 규칙

`nullable = false` 인 필드는 **원시 타입**(`int` · `boolean` · `long`)을 쓴다.
박싱 타입에 `null` 이 들어가면 `IS NULL` 조회가 되어 **조용히 틀린다.**

**단 요청 DTO 는 예외다.** `Integer` 를 유지해야 `@NotNull` 이 "누락" 을 감지한다.
`int` 로 바꾸면 필드가 없을 때 0 으로 역직렬화되어 검증을 통과한다.

### SQL 예약어

`order` · `year` · `group` 은 예약어다. 컬럼명을 분리하고 **이유를 주석에 남긴다.**

| 필드 | 컬럼 |
|---|---|
| `order` | `display_order` · `question_order` |
| `year` | `generation_year` |
| (테이블) `Group` | `user_group` |

JSON 키는 필드명 그대로 나간다 — 컬럼명을 바꿔도 API 는 영향받지 않는다.

### JSON 컬럼

새로 만들 때는 `AttributeConverter` 대신 **`@JdbcTypeCode(SqlTypes.JSON)` 을 우선 검토한다**
(Spring 이 설정한 `ObjectMapper` 가 쓰인다).

> 기존 코드는 아직 전부 `AttributeConverter` 다 (`QuestionOptionListConverter` ·
> `SelectedOptionsConverter` · `SubmissionTypeSetConverter` · `TechStackListConverter`).
> 바꾸라는 뜻은 아니고, **새로 추가할 때** 적용하라는 뜻이다.

### 검증은 엔티티가 아니라 서비스에서

엔티티는 받은 값을 그대로 담는다.

---

## 서비스

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)      // 클래스 레벨
public class ApplicationQuestionService {

  @Transactional                      // 쓰기 메서드에만
  public Result createQuestion(...) { ... }
}
```

### interface 를 만들지 않는다

구현 클래스 하나다. `...Service` + `...ServiceImpl` 로 나누지 않는다.

**예외는 [크로스 도메인 계약](ARCHITECTURE.md#크로스-도메인-계약) 하나뿐이다.**
다른 도메인이 내 데이터를 읽어야 할 때만 제공자 패키지에 조회 인터페이스를 둔다.

### 엔티티를 컨트롤러로 내보내지 않는다

서비스는 **Result DTO** 를 반환한다. 엔티티를 그대로 내보내면 지연로딩 연관이 붙는 순간
`LazyInitializationException` 이 난다.

### 파라미터가 많으면 Command record 로 묶는다

파라미터가 3개를 넘거나 **같은 타입이 인접하면** 묶는다.

```java
create(String name, String guideline, int score)   // ❌ String 두 개가 붙어 있다
```

순서를 바꿔도 컴파일이 통과하므로, 틀렸을 때 런타임에야 알게 된다.

### 명세서의 검증 순서를 주석으로 옮겨 적는다

명세서에 검증 순서가 정의돼 있으면 그 순서를 주석으로 가져온다. 구현 누락을 막는다.
`ApplicationService` 의 제출 검증이 그 예다.

---

## 컨트롤러

얇게 유지한다. 요청 DTO 검증(`@Valid`) 과 서비스 호출, `ApiResponse` 로 감싸기까지.

- **`@Transactional` 을 붙이지 않는다** (ArchUnit 이 막는다)
- **Repository 를 직접 호출하지 않는다** (ArchUnit 이 막는다)
- try-catch 하지 않는다 — `GlobalExceptionHandler` 가 받는다

메서드 배치 순서: 엔드포인트가 같으면 메서드와 무관하게 모아두고,
그 안에서 `GET → POST → PUT → PATCH → DELETE` 순으로 쓴다.

---

## DTO · 검증

```java
public record ApplicationQuestionRequest(
    @NotNull QuestionType type,
    @NotBlank @Size(max = 500) String content,      // 엔티티 @Column(length = 500) 과 일치
    @Positive @Max(2000) Integer maxLength
) { }
```

- **`@Size` 상한을 엔티티 `@Column(length)` 와 반드시 일치시킨다.**
  없으면 초과 입력이 검증을 통과해 INSERT 까지 가고 `DataIntegrityViolationException` →
  **400 이어야 할 응답이 500 이 된다.**
- 숫자 필드에 범위 검증(`@Positive` · `@Min` · `@Max`)을 빠뜨리지 않는다
- 요청과 응답 DTO 를 **공유하지 않는다.** `...Request` / `...Result` 로 나눈다
- 중첩 DTO 를 검증하려면 필드에 `@Valid` 가 필요하다

DTO 는 기능별 클래스 안에 `record` 로 모아 쓴다.

---

## 에러

- 비즈니스 검증 실패에는 **도메인 `ErrorCode`** 를 쓴다. `VALIDATION_FAILED` 를 재사용하지 않는다
- **같은 `code` 에 서로 다른 `message` 를 쓰지 않는다**
- `message` 는 fallback 이다. 프론트가 분기 근거로 쓰는 것은 **`code` 뿐**이다

도메인마다 `@RestControllerAdvice` 를 둘 때는 **범위를 반드시 좁힌다.** Spring 은 먼저 매칭된
advice 에서 반환하고, 구체성 우선순위는 **하나의 advice 안에서만** 적용된다.

현재 도메인 advice 는 `FileExceptionHandler` 하나이고, `basePackages` 로 범위를 좁혀 해결했다.

```java
@RestControllerAdvice(basePackages = "com.getit.domain.file.controller")
```

`@Order` 로 우선순위를 주는 방법도 있다. 둘 중 하나는 반드시 해야 한다.

---

## 시간

서버 타임존은 `Asia/Seoul` 로 고정돼 있다 (JVM · Jackson · Hibernate JDBC 3중).

| 자리 | 타입 |
|---|---|
| 엔티티 | `LocalDateTime` |
| 응답 DTO | **`OffsetDateTime`** — 명세서가 `+09:00` 오프셋을 명시한다 |

### `LocalDateTime.now()` 를 서비스에서 직접 부르지 않는다

**`Clock` 을 주입받는다.** 시간에 따라 결과가 갈리는 로직(마감 검증 · D-day)은
`Clock` 없이는 경계값을 테스트할 수 없고, 서비스가 계산한 `now` 와 테스트가 만든 `now`
사이에 자정이 끼면 **간헐적으로 실패한다.**

빈 이름은 **`recruitmentClock`** 이다 (`domain/recruitment/config/RecruitmentClockConfig`).
이름은 recruitment 지만 **프로젝트 공용**으로 쓰고 있고, 현재 8개 서비스가 주입받는다 —
auth · recruitment · user · dashboard · setting 전반.

```bash
grep -rln 'Clock' src/main/java/com/getit/domain --include='*Service*.java'
```

D-day 는 **항상 서버에서 계산해 내려준다.** 클라이언트 시계를 믿지 않는다.

---

## 테스트

테스트는 **같은 PR 에 넣는다.** 분리는 안티패턴이다 — 나중에 올리기로 하면 실제로는
올라오지 않고, 롤백 시 코드와 테스트가 어긋나 CI 가 깨진다.

### 레이어별 방식

| 레이어 | 방식 |
|---|---|
| 컨트롤러 | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@Transactional` |
| 서비스 | `@SpringBootTest` + `@Transactional` |
| 저장소 | `@DataJpaTest` + `@Import(JpaAuditingConfig.class)` |
| 엔티티 | 순수 단위 테스트 (스프링 컨텍스트 없음) |
| 스키마 | `@Tag("schema")` — `./gradlew schemaTest` 로만 돈다 |

### 네이밍

- 위치는 프로덕션 패키지를 **그대로 미러링**한다
- 클래스명은 `{대상}Test`
- `@Nested` + `@DisplayName` 으로 API 단위 그룹화, **DisplayName 은 한국어 서술문**

### 반드시 포함할 것

- **인가 테스트** — 토큰 없으면 401, 권한 없으면 403.
  상태 코드 하나에 고정하지 말고 **401/403 여부**로 검증한다
- **활성 기수가 아닌 리소스 접근 시 404**
- **경계값** — 시작·종료 시각을 양쪽 다
- **실패 경로.** 성공 케이스만 있는 테스트는 미완성이다
- 예외 검증에는 **롤백까지** 확인한다 (`실패한 요청은 저장되지 않아야 한다`)
- **`AttributeConverter` · JSON 컬럼은 DB 왕복 테스트.**
  `flush()` + `entityManager.clear()` 를 하지 않으면 1차 캐시에서 값이 나와
  **읽기 경로가 한 번도 실행되지 않는다**

커버리지 게이트는 걸지 않는다. 숫자를 맞추려고 의미 없는 테스트를 쓰게 된다.

---

## 포맷 · 네이밍

| | |
|---|---|
| 들여쓰기 | 스페이스 2칸 (줄바꿈된 연속 줄은 +4칸) |
| 줄 길이 | 120자 |
| 클래스 길이 | 300줄 |
| 포맷 자동 수정 | `./gradlew spotlessApply` |

| 대상 | 규칙 |
|---|---|
| Java 변수 · 메서드 | `camelCase` |
| 클래스 · 인터페이스 | `PascalCase` — 역할에 맞는 접미사 (`...Controller` · `...Service`) |
| 패키지 | 소문자 |
| SQL | `snake_case` |
| 상수 | `UPPER_SNAKE_CASE` |
| boolean | `is` · `has` · `should` 접두사 |

> Spotless 는 **의도적으로 얕게** 설정돼 있다. 현재 코드가 그대로 통과하는 수준이다.
> `googleJavaFormat` 이나 `importOrder` 를 켜면 80개 파일이 한 번에 바뀌어 진행 중인
> 브랜치가 전부 충돌한다. 강화는 작업이 적은 시점에 별도 PR 로 한다.
>
> 그래서 **120자와 300줄은 자동으로 강제되지 않는다.** 리뷰에서 본다.
> 현재 300줄을 넘는 파일은 3개다.

## 주석

**무엇(What)이 아니라 왜(Why)를 쓴다.** 코드만 읽었을 때 납득이 필요한 지점이
스스로 느껴지면 적극적으로 쓴다.

이 저장소는 이걸 잘 하고 있다. 좋은 예로 `V34__add_privacy_consent.sql` 의 머리말이나
`Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO` 의 주석을 읽어볼 것 —
**"왜 이렇게 됐는지" 와 "이렇게 안 하면 뭐가 깨지는지" 가 함께 적혀 있다.**

컨트롤러·서비스 주석에는 해당 API 명세 번호를 남긴다 — `(API 명세서 6.3 · 6.4)`.
