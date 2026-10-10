# GETIT 백엔드 — AI 코딩 에이전트 지침

경북대 창업 IT 동아리 **GETIT** 통합 운영 플랫폼의 백엔드입니다.
이 문서는 `GETIT_Site_BE` 레포지토리 전체에 적용됩니다.

## 규칙은 `docs/` 에 있습니다

**이 파일에 코드 규칙을 복사하지 마십시오.** 같은 규칙을 두 곳에 두면 반드시 갈라집니다
(실제로 이 저장소에서 API 엔드포인트 개수가 세 군데에 서로 다르게 적혀 있던 적이 있습니다).

코드를 쓰거나 리뷰할 때 아래를 기준으로 삼습니다.

| 문서 | 내용 |
|---|---|
| [`docs/CONVENTIONS.md`](../docs/CONVENTIONS.md) | 엔티티 · 서비스 · 컨트롤러 · DTO · 시간 · 테스트 규약 |
| [`docs/PITFALLS.md`](../docs/PITFALLS.md) | 자주 나는 사고 + **리뷰 체크리스트** |
| [`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md) | 패키지 경계 · 크로스 도메인 계약 · 권한 |
| [`docs/GLOSSARY.md`](../docs/GLOSSARY.md) | 도메인 용어 |
| [`docs/WORKFLOW.md`](../docs/WORKFLOW.md) | 브랜치 · 커밋 · PR · CI · CD |
| [`src/main/resources/db/migration/README.md`](../src/main/resources/db/migration/README.md) | 마이그레이션 번호 예약 표 |

여섯 개 구조 규칙은 `src/test/java/com/getit/architecture/ArchitectureTest.java` 로
**강제**됩니다. 어기면 빌드가 깨집니다.

## 언어

응답 · 주석 · 커밋 메시지는 **한국어**로 작성합니다. 코드 식별자는 영어입니다.

주석은 **무엇(What)이 아니라 왜(Why)** 를 씁니다. 이 저장소는 결정의 배경과
"이렇게 안 하면 뭐가 깨지는지" 를 주석에 남기는 관례가 있습니다.
`V34__add_privacy_consent.sql` 의 머리말이 그 기준입니다.

## PR 을 리뷰할 때

[`docs/PITFALLS.md`](../docs/PITFALLS.md) 의 **리뷰 체크리스트 15개**를 우선 확인합니다.
전부 이 저장소에서 실제로 반복해서 발생한 문제입니다.

사고가 가장 많이 나는 순서대로:

1. **마이그레이션** — 번호 예약 · 인덱스 누락 · 순서 의존
2. **활성 기수 스코프** — 기수에 속한 리소스의 수정·삭제
3. **`order` 재정렬** — 삭제 후 1..n 연속 유지
4. **N+1 과 tie-breaker** — 목록 조회와 페이징
5. **DTO `@Size` ↔ 엔티티 `@Column(length)`** — 불일치 시 400 이 500 이 됩니다

## 명세와 구현이 다를 때

**구현된 API 의 기준은 OpenAPI 문서입니다** — 앱을 띄우고 `/v3/api-docs` 또는
`/swagger-ui.html` 을 봅니다. **손으로 쓴 엔드포인트 목록을 기준으로 삼지 마십시오.**
노션의 API 명세서는 기획 시점 스냅샷이고 구현과 다릅니다.

컨트롤러 · 서비스 주석에는 기획 명세 번호를 남기는 관례가 있습니다 — `(API 명세서 6.3 · 6.4)`.

기획 명세와 구현이 다르면 **구현을 바꾸기 전에 명세 결함인지 먼저 확인하십시오.**
명세에 없는 필드를 임의로 추가하거나, 있는 필드를 임의로 빼지 않습니다.
판단이 필요하면 PR 본문의 「리뷰 포인트」에 적고 넘깁니다.

## 작업 범위

- **패키지 = 소유권.** 자기 작업과 무관한 패키지를 수정하지 않습니다
- 전 도메인이 공유하는 파일(`SecurityConfig` · `application*.yml` · `global/dto` ·
  `GlobalExceptionHandler` · `User` · `Generation`)은 혼자 고치지 않고 **먼저 알립니다**
  ([`docs/ARCHITECTURE.md`](../docs/ARCHITECTURE.md#함께-쓰는-파일--바꾸기-전에-물어본다))
- 테스트는 **같은 PR** 에 넣습니다. **실패 경로를 반드시** 검증합니다
