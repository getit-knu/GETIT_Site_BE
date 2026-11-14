package com.getit.domain.festa.dto;

import java.util.List;

/**
 * 내 가상 투자 현황. (이슈 #222)
 *
 * @param unit 투자금을 넣는 단위(원). FE 가 입력 단위로 그대로 쓴다
 * @param maskedPhoneNumber {@code 010-****-1234} 형태. 아직 번호를 낸 적이 없으면 null
 */
public record FestaPortfolioResult(
    long budget,
    long unit,
    long remaining,
    String maskedPhoneNumber,
    List<Investment> investments
) {

  public record Investment(Long projectId, long amount) { }
}
