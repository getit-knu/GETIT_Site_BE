## 작업 내용

<!-- 무엇을 왜 했는지 적습니다. -->

## 관련 이슈

close #

## 구현한 API

<!-- 명세서 번호와 엔드포인트. 해당 없으면 지웁니다. -->

| # | Method | Endpoint |
|---|---|---|
| 3.3 | PUT | `/applications/me/draft` |

## 리뷰 포인트

<!-- 특히 봐줬으면 하는 부분, 고민한 지점, 대안이 있었던 선택 등 -->

## 체크리스트

- [ ] 내 소유 패키지 안에서만 작업했다 (다른 패키지가 필요했다면 아래에 적었다)
- [ ] 다른 도메인의 데이터가 필요할 때 Repository 를 직접 참조하지 않고 인터페이스를 거쳤다
- [ ] 응답을 `ApiResponse` envelope 으로 감쌌다
- [ ] 도메인 `ErrorCode` enum 에 에러 코드를 정의했다 (`CommonErrorCode` 에 추가하지 않았다)
- [ ] `./gradlew build` 가 통과한다
- [ ] **프로덕션 코드** 변경이 500줄 이하다 (테스트는 세지 않는다. 넘으면 쪼갠다)
- [ ] 커밋 메시지가 컨벤션을 따른다 (`feat(recruitment): ...`)

## 다른 사람 소유 파일을 건드렸다면

<!--
무엇을 왜 건드렸는지 적습니다. 대상:
SecurityConfig · application*.yml · global/dto · GlobalExceptionHandler · User · Generation 엔티티
자세한 내용은 docs/ARCHITECTURE.md 의 "함께 쓰는 파일" 참조.
해당 없으면 이 섹션을 지웁니다.
-->
