package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaPortfolioRequest;
import com.getit.domain.festa.dto.FestaPortfolioResult;
import com.getit.domain.festa.entity.FestaInvestment;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaInvestmentRepository;
import com.getit.global.exception.BusinessException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모의투자 FESTA 가상 투자. (이슈 #222)
 *
 * <p>모든 로그인 사용자(GUEST 포함)가 {@link #BUDGET} 을 받아 출전 프로젝트에 {@link #UNIT} 단위로
 * 나눠 넣는다. 저장은 사용자의 기존 투자를 요청 내용으로 통째로 바꾼다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FestaPortfolioService {

  public static final long BUDGET = 10_000_000L;
  public static final long UNIT = 100_000L;

  private final FestaInvestmentRepository investmentRepository;
  private final FestaParticipation participation;

  /** 토글이 꺼져 있어도 내 현황은 볼 수 있다. 고칠 수만 없다. */
  @Transactional(readOnly = true)
  public FestaPortfolioResult getPortfolio(Long userId) {
    return toResult(userId);
  }

  public FestaPortfolioResult savePortfolio(Long userId, FestaPortfolioRequest request) {
    participation.ensureOpen();
    List<FestaPortfolioRequest.Investment> investments = request.investments();
    validateAmounts(investments);
    validateProjects(userId, investments);
    participation.savePhoneNumber(userId, request.phoneNumber(), request.privacyConsent());

    investmentRepository.deleteByUserId(userId);
    investmentRepository.saveAll(investments.stream()
        .filter(investment -> investment.amount() > 0)
        .map(investment -> FestaInvestment.of(userId, investment.projectId(), investment.amount()))
        .toList());
    return toResult(userId);
  }

  private void validateAmounts(List<FestaPortfolioRequest.Investment> investments) {
    Set<Long> seen = new HashSet<>();
    long sum = 0;
    for (FestaPortfolioRequest.Investment investment : investments) {
      if (!seen.add(investment.projectId())) {
        throw new BusinessException(FestaErrorCode.DUPLICATE_PROJECT);
      }
      long amount = investment.amount();
      if (amount < 0 || amount % UNIT != 0) {
        throw new BusinessException(FestaErrorCode.INVALID_AMOUNT);
      }
      sum += amount;
      if (sum > BUDGET) {
        throw new BusinessException(FestaErrorCode.BUDGET_EXCEEDED);
      }
    }
  }

  /** 금액이 0 이어도 출전하지 않았거나 우리 조 프로젝트면 거부한다. 화면이 보낼 수 없는 요청이다. */
  private void validateProjects(Long userId, List<FestaPortfolioRequest.Investment> investments) {
    investments.forEach(investment -> participation.validateProject(userId, investment.projectId()));
  }

  private FestaPortfolioResult toResult(Long userId) {
    List<FestaPortfolioResult.Investment> investments =
        investmentRepository.findByUserIdOrderByProjectId(userId).stream()
            .map(investment -> new FestaPortfolioResult.Investment(
                investment.getProjectId(), investment.getAmount()))
            .toList();
    long invested = investments.stream().mapToLong(FestaPortfolioResult.Investment::amount).sum();
    return new FestaPortfolioResult(BUDGET, UNIT, BUDGET - invested,
        participation.maskedPhoneNumber(userId), investments, participation.ownProjectIds(userId));
  }
}
