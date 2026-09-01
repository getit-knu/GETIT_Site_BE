package com.getit.domain.auth.dto;

import jakarta.validation.constraints.AssertTrue;

/**
 * 개인정보 수집·이용 동의 요청. (이슈 #203)
 *
 * <p>본문 자체가 선택이다. 이슈가 "요청 본문 없음(또는 최소한만)"으로 확정했고, 동의 화면에서
 * 넘어왔다는 사실 외에 서버가 받아야 할 값이 없다. 본문 없이 호출하면 동의한 것으로 본다.
 *
 * <p>본문을 보내는 경우에만 {@code privacyConsent} 가 {@code false} 인지 확인한다. {@code Boolean}
 * 을 유지하는 이유는 {@code boolean} 으로 바꾸면 필드가 없을 때 false 로 역직렬화돼 "생략"과
 * "동의하지 않음"이 구분되지 않기 때문이다. {@code null}(생략)은 통과시키고 {@code false} 만
 * 막는다.
 *
 * <p>동의 철회는 이 엔드포인트로 하지 않는다. 철회는 탈퇴(9.3)와 함께 다뤄야 할 별개의 흐름이다.
 */
public record PrivacyConsentRequest(
    @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다.")
    Boolean privacyConsent
) { }
