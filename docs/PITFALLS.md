# 자주 나는 사고

**PR 을 올리기 전에** 훑는다. 코드를 쓰기 전 규칙은 [CONVENTIONS.md](CONVENTIONS.md) 에 있다.

여기 적힌 것은 가상의 위험이 아니라 **이 저장소에서 실제로 반복해서 터진 일**이다.
횟수가 적힌 항목은 그만큼 났다는 뜻이다.

맨 아래 [리뷰 체크리스트](#리뷰-체크리스트)만 따로 봐도 된다.

---

## 1. DB 마이그레이션 — 사고가 가장 많이 나는 곳

### 번호는 이슈에서 선점한다 (충돌 4회)

[`db/migration/README.md`](../src/main/resources/db/migration/README.md) 의 예약 표에
등록한 뒤 쓴다. **PR 리뷰에서 번호를 정하지 않는다** — 여러 PR 이 동시에 열리면 반드시 충돌한다.

- 결번은 무해하다. 되살리려 하지 않는다
- 두 PR 의 번호가 충돌하면 **머지 순서를 먼저 정하고** 나중 머지 쪽이 올린다

### 순서에 의존하는 마이그레이션을 쓴다

번호가 어긋나는 사고는 이미 한 번 운영을 내렸다. **V19 → V22 가 먼저 적용된 뒤
V20 · V21 이 머지되자 Flyway 가 `resolved migration not applied` 로 기동을 거부했다.**

그래서 dev · prod 는 **`out-of-order: true` 로 운영한다.** 번호가 낮은 마이그레이션이
뒤늦게 들어와도 적용된다.

> ⚠️ `.github/copilot-instructions.md` 에는 아직 "`outOfOrder` 는 켜지 않습니다" 로 적혀 있다.
> **낡은 내용이다.** 실제 설정은 `application-dev.yml` · `application-prod.yml` 을 보면 된다.

대신 **대가가 있다. 마이그레이션끼리 순서에 의존하지 않게 작성해야 한다.**

- 앞선 마이그레이션이 만든 테이블을 전제하는 변경은 **같은 PR 안에 함께 둔다**
- 다른 PR 의 마이그레이션이 먼저 들어왔다고 가정하지 않는다

`out-of-order` 가 켜져 있으니 번호를 왜 예약하냐고 생각할 수 있다.
**순서가 어긋나도 적용은 되지만, 같은 번호를 두 사람이 쓰면 그건 여전히 충돌이다.**

### 새 테이블에는 인덱스를 함께 넣는다 (누락 4회)

**이 프로젝트에서 가장 자주 누락되는 항목이다.** 조회 조건과 정렬 키를 보고 잡는다.

```sql
KEY idx_lecture_generation_week (generation_id, week, id)
```

유니크 제약이 조회 패턴의 **선행 컬럼을 덮으면** 별도 인덱스가 필요 없다.
`uk_application_answer_question (application_id, question_id)` 는 `findByApplicationId` 를 커버한다.

### 그 밖에

- 로컬(`ddl-auto: update`)에서 Hibernate 가 생성한 DDL 을 옮기되 **enum CHECK 제약은 제거한다**
- 시드 데이터에 **`id` 를 하드코딩하지 않는다.** `AUTO_INCREMENT` 시작값은 서버 설정에 따라 다르다

  ```sql
  SELECT c.id FROM college c WHERE c.name = '...'   -- 조회해서 넣는다
  ```
- **스키마 변경과 데이터 적재는 다른 마이그레이션 번호로 분리한다**
- 머지된 마이그레이션은 **절대 수정하지 않는다.** 새 번호를 만든다

---

## 2. 활성 기수 스코프를 빠뜨린다

기수에 속한 리소스를 수정·삭제할 때 **활성 기수 소속인지 확인하지 않으면,
ID 만 아는 사람이 지난 기수 데이터를 고칠 수 있다.**

서비스에서 조회한 뒤 비교하는 방식보다 **저장소 메서드로 스코프하는 쪽을 쓴다.**
빠뜨릴 여지가 없다.

```java
// ✅ 저장소에서 한 번에
Optional<EvaluationCriterion> findByIdAndGenerationId(Long id, Long generationId);

// ❌ 조회 후 비교 — 다음 사람이 빠뜨린다
var c = repo.findById(id).orElseThrow();
if (!c.getGenerationId().equals(activeGenerationId)) { ... }
```

쓰기 작업에는 `GenerationQueryService.findActiveForWrite()` 를 쓴다.
활성화 전환과 경쟁할 수 있어 잠금이 필요하다.

테스트에 **활성 기수가 아닌 리소스 접근 시 404** 를 넣는다.

---

## 3. `order` 를 쓰는 리소스에서 재정렬을 빠뜨린다

`order` 컬럼을 쓰는 리소스는 **1..n 연속**을 불변식으로 유지한다.

- 새 항목의 order 는 `count + 1` 이 아니라, **삭제 시 뒤 항목을 당긴 뒤**의 개수 기준이다
- 삭제 후 재정렬을 하지 않으면 order 중복이 생기고 **정렬 결과가 비결정적이 된다**
- 순서 변경 API 는 **활성 기수의 전체 집합과 정확히 일치**하는지 검증한다
  — 부분 목록 · 중복 ID · 타 기수 ID 를 한 번에 막는다

---

## 4. 쿼리

### tie-breaker 를 빠뜨린다

**정렬에는 항상 마지막 기준을 넣는다.**

```java
order by l.week asc, l.id asc
```

페이징에서 정렬이 불안정하면 **같은 행이 두 번 나오거나 누락된다.**
테스트에서는 잘 안 보이고 데이터가 쌓인 운영에서 드러난다.

### N+1

목록을 조회한 뒤 각 항목마다 다시 조회하지 않는다. **`findAllBy...In` + `toMap`** 을 쓴다.

```java
// ❌ 루프 안의 findById · save
for (var item : items) { var user = userRepo.findById(item.getUserId()); }

// ✅ 한 번에 긁어서 Map 으로
Map<Long, String> names = userQueryService.findNamesByIds(userIds);
```

### soft delete 를 빠뜨린다

soft delete 를 쓰는 엔티티는 조회 시 **`deletedAt is null`** 을 빠뜨리지 않는다.
빠뜨리면 지운 데이터가 다시 나온다.

### 파생 쿼리 이름이 겹친다

`order` 같은 필드명과 겹쳐 헷갈리면 `@Query` 로 명시한다.

---

## 5. `@Transactional` 이 안 걸린 줄 모른다

**`@Transactional` 은 프록시 기반이다.** 같은 클래스 안에서 호출하면(self-invocation)
**적용되지 않는다.**

```java
public void outer() {
  inner();            // ❌ inner 의 @Transactional 이 무시된다
}

@Transactional
public void inner() { ... }
```

조용히 안 걸리기 때문에 테스트에서 롤백까지 확인해야 잡힌다.

---

## 6. `LazyInitializationException`

서비스가 **엔티티를 컨트롤러로 내보내면** 지연로딩 연관이 붙는 순간 터진다.
**Result DTO 로 변환해서 반환한다.**

---

## 7. 400 이어야 할 응답이 500 이 된다

**DTO 의 `@Size` 상한이 엔티티 `@Column(length)` 와 다를 때** 생긴다.
초과 입력이 검증을 통과해 INSERT 까지 가고 `DataIntegrityViolationException` 이 난다.

엔티티 길이를 바꿨으면 **DTO 도 같이 바꾼다.**

---

## 8. 박싱 타입에 `null` 이 들어가 조용히 틀린다

`nullable = false` 인 엔티티 필드는 **원시 타입**을 쓴다.
박싱 타입에 `null` 이 들어가면 `IS NULL` 조회가 되어 결과가 조용히 바뀐다.

**요청 DTO 는 반대다.** `Integer` 를 유지해야 `@NotNull` 이 누락을 잡는다.
`int` 면 필드가 없을 때 0 으로 역직렬화되어 검증을 통과한다.

---

## 9. `Map.get()` 결과를 null 검사 없이 쓴다

N+1 을 피하려고 `toMap` 을 쓰다 보면 생긴다. **키가 없을 수 있다** —
soft delete 된 사용자, 다른 기수 데이터 등.

---

## 10. `LocalDateTime.now()` 를 직접 부른다

시간에 따라 결과가 갈리는 로직은 `Clock` 없이 **경계값을 테스트할 수 없다.**
서비스가 계산한 `now` 와 테스트가 만든 `now` 사이에 자정이 끼면 **간헐적으로 실패한다.**

`recruitmentClock` 빈을 주입받는다. 자세한 내용은 [CONVENTIONS.md](CONVENTIONS.md#시간).

---

## 11. 브랜치명에 `#` 을 붙인다

```
feat/203-privacy-consent      ✅
feat/#203-privacy-consent     ❌
```

`#` 은 셸과 일부 도구에서 주석으로 해석된다.

---

## 12. PR 이 500줄을 넘는다

**프로덕션 코드 500줄 이하.** 테스트는 세지 않는다.

```bash
git diff --numstat main...HEAD -- src/main | awk '{s+=$1} END {print s" 줄"}'
```

넘으면 쪼갠다. 자연스러운 분할선은 **① 엔티티 + 마이그레이션 → ② 서비스 + 컨트롤러** 다.
**다른 PR 의 후속 작업이 섞여 있으면 그것부터 떼어낸다.**

---

## 리뷰 체크리스트

PR 을 올리기 전에 스스로 본다. **전부 실제로 반복해서 발생한 문제다.**

- [ ] 새 테이블의 조회 조건·정렬 키에 **인덱스**가 있는가 *(누락 4회)*
- [ ] 마이그레이션 번호가 **예약 표**에 등록됐고 기존 최대 번호보다 큰가 *(충돌 4회)*
- [ ] 기수에 속한 리소스의 수정·삭제에 **활성 기수 스코프**가 걸렸는가
- [ ] `order` 를 쓰는 리소스에서 **삭제 후 재정렬**을 하는가
- [ ] DTO `@Size` 가 엔티티 `@Column(length)` 와 **일치**하는가 *(불일치 시 500 발생)*
- [ ] 비즈니스 검증 실패에 **도메인 `ErrorCode`** 를 썼는가 *(`VALIDATION_FAILED` 재사용 금지)*
- [ ] `nullable = false` 필드가 **원시 타입**인가 *(요청 DTO 는 예외)*
- [ ] 목록 조회에 **N+1** 이 없는가 *(루프 안의 `findById` · `save`)*
- [ ] 정렬에 **tie-breaker** 가 있는가
- [ ] 서비스가 **엔티티가 아닌 Result DTO** 를 반환하는가
- [ ] `Map.get()` 결과를 **null 검사 없이** 쓰는 곳이 없는가
- [ ] 다른 도메인의 **Repository 직접 주입**이 없는가
- [ ] `LocalDateTime.now()` 직접 호출 대신 **`Clock`** 을 주입했는가
- [ ] 브랜치명에 **`#`** 이 없는가
- [ ] 프로덕션 코드가 **500줄** 이하인가

마지막으로:

```bash
./gradlew spotlessApply && ./gradlew build
```
