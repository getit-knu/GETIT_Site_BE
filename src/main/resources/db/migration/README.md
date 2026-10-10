# 마이그레이션 번호 예약 표

> **다음에 쓸 번호: `V35`**

스키마를 건드리는 작업을 시작하면 **코드를 쓰기 전에** 아래 예약 표에 한 줄 추가하고
그 번호를 쓴다. 등록은 PR 이 아니라 **작업을 시작할 때** 한다.

**PR 리뷰에서 번호를 정하지 않는다.** 여러 PR 이 동시에 열리면 반드시 충돌한다 —
이 저장소에서 실제로 4회 났다.

---

## 예약 중

| 번호 | 이슈 | 내용 | 예약자 |
|---|---|---|---|
| | | | |

*(작업을 시작할 때 여기에 한 줄 추가한다. 머지되면 아래 "적용됨" 으로 옮긴다.)*

---

## 규칙

### 번호

- `V{번호}__{내용}.sql` — 밑줄 **두 개**다
- 두 PR 의 번호가 충돌하면 **머지 순서를 먼저 정하고** 나중 머지 쪽이 번호를 올린다
- dev · prod 는 **`out-of-order: true`** 다. 번호가 낮은 마이그레이션이 뒤늦게 들어와도 적용된다.
  이 설정이 없던 시절 **V19 → V22 적용 후 V20 · V21 이 머지돼 운영이 내려갔다**
- 그 대가로 **마이그레이션끼리 순서에 의존하지 않게 써야 한다.**
  앞선 마이그레이션이 만든 테이블을 전제하는 변경은 **같은 PR 안에 함께 둔다**
- 예약했다가 작업을 접었으면 **표에서 지운다.** 결번은 무해하니 되살리려 하지 않는다

### 내용

- 머지된 마이그레이션은 **절대 수정하지 않는다.** 새 번호를 만든다
- **새 테이블에는 인덱스를 함께 넣는다.** 이 프로젝트에서 가장 자주 누락되는 항목이다 (4회)
- 로컬(`ddl-auto: update`)에서 Hibernate 가 생성한 DDL 을 옮기되 **enum CHECK 제약은 제거한다**
- 시드 데이터에 **`id` 를 하드코딩하지 않는다.** `AUTO_INCREMENT` 시작값은 서버 설정에 따라 다르다

  ```sql
  SELECT c.id FROM college c WHERE c.name = '...'
  ```
- **스키마 변경과 데이터 적재는 다른 번호로 분리한다** (V20 ↔ V21 · V8 ↔ V24 가 그 예다)
- SQL 예약어를 피한다 — `order` → `display_order`, `year` → `generation_year`, `group` → `user_group`
- **왜 이 변경이 필요한지 머리말에 적는다.** `V34__add_privacy_consent.sql` 이 좋은 예다

### 검증

```bash
docker compose up -d
./gradlew schemaTest     # 빈 MySQL 에 Flyway 실행 → ddl-auto: validate 로 엔티티와 대조
```

**엔티티만 바꾸고 마이그레이션을 빼먹으면 여기서 잡힌다.** H2 로는 검증할 수 없어 실제 MySQL 을 띄운다.

> ⚠️ **이 검증이 못 잡는 것** — 매번 빈 DB 에서 시작하므로 **마이그레이션 간 순서 의존은
> 드러나지 않는다.** 빈 DB 에서는 전부 번호순으로 적용되기 때문이다.
> 순서에 의존하는 변경을 쪼개 올리면 이미 돌아가는 dev·prod 에서만 터진다.

---

## 적용됨

| 번호 | 내용 |
|---|---|
| V1 | `init_auth_schema` |
| V2 | `create_recruitment_schedule` |
| V3 | `create_file_asset_schema` |
| V4 | `create_category_schema` |
| V5 | `create_application_question` |
| V6 | `create_evaluation_criterion` |
| V7 | `create_application_schema` |
| V8 | `create_college_major_schema` |
| V9 | `create_lecture_schema` |
| V10 | `create_evaluation_score` |
| V11 | `create_user_group_schema` |
| V12 | `create_assignment_submission_schema` |
| V13 | `create_curriculum_schema` |
| V14 | `create_staff_schema` |
| V15 | `create_feedback_schema` |
| V16 | `add_lecture_published_query_index` |
| V17 | `add_lecture_published_at` |
| V18 | `create_event_schema` |
| V19 | `create_faq_schema` |
| V20 | `create_feature_toggle_schema` |
| V21 | `seed_feature_toggle` |
| V22 | `create_qna_schema` |
| V23 | `create_project_schema` |
| V24 | `seed_college_major_full_data` |
| V25 | `create_activity_photo_schema` |
| V26 | `add_evaluator_to_evaluation_score` |
| V27 | `add_sns_links_to_staff` |
| V28 | `add_criterion_fk_to_evaluation_score` |
| V29 | `add_profile_edit_to_users` |
| V30 | `add_status_to_project` |
| V31 | `add_apply_enabled_to_recruitment_schedule` |
| V32 | `add_reject_reason_to_project` |
| V33 | `add_group_id_to_project` |
| V34 | `add_privacy_consent` |

현재 번호를 다시 확인하려면:

```bash
ls src/main/resources/db/migration/V*.sql | sed -E 's/.*V([0-9]+)__.*/\1/' | sort -n | tail -1
```
