package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaPortfolioRequest;
import com.getit.domain.festa.dto.FestaPortfolioResult;
import com.getit.domain.festa.entity.FestaInvestment;
import com.getit.domain.festa.entity.FestaInvestor;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaInvestmentRepository;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.project.service.ProjectQueryService;
import com.getit.domain.project.service.ProjectView;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureQueryService;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.service.MemberGroupService;
import com.getit.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

  private static final Pattern PHONE_NUMBER = Pattern.compile("010-\\d{4}-\\d{4}");

  private final FestaInvestmentRepository investmentRepository;
  private final FestaInvestorRepository investorRepository;
  private final FeatureQueryService featureQueryService;
  private final ProjectQueryService projectQueryService;
  private final MemberGroupService memberGroupService;
  private final Clock clock;

  /** 토글이 꺼져 있어도 내 현황은 볼 수 있다. 고칠 수만 없다. */
  @Transactional(readOnly = true)
  public FestaPortfolioResult getPortfolio(Long userId) {
    return toResult(userId);
  }

  public FestaPortfolioResult savePortfolio(Long userId, FestaPortfolioRequest request) {
    ensureOpen();
    List<FestaPortfolioRequest.Investment> investments = request.investments();
    validateAmounts(investments);
    validateProjects(userId, investments);
    savePhoneNumber(userId, request);

    investmentRepository.deleteByUserId(userId);
    investmentRepository.saveAll(investments.stream()
        .filter(investment -> investment.amount() > 0)
        .map(investment -> FestaInvestment.of(userId, investment.projectId(), investment.amount()))
        .toList());
    return toResult(userId);
  }

  private void ensureOpen() {
    boolean open = featureQueryService.findAll().stream()
        .anyMatch(feature -> feature.key() == FeatureKey.MOCK_INVESTMENT && feature.enabled());
    if (!open) {
      throw new BusinessException(FestaErrorCode.FESTA_CLOSED);
    }
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
    Map<Long, ProjectView> entries = projectQueryService.findFestaEntries().stream()
        .collect(Collectors.toMap(ProjectView::id, Function.identity()));
    Long myGroupId = findMyGroupId(userId);

    for (FestaPortfolioRequest.Investment investment : investments) {
      ProjectView project = entries.get(investment.projectId());
      if (project == null) {
        throw new BusinessException(FestaErrorCode.PROJECT_NOT_ENTERED);
      }
      if (myGroupId != null && myGroupId.equals(project.groupId())) {
        throw new BusinessException(FestaErrorCode.OWN_GROUP_PROJECT);
      }
    }
  }

  /** 번호를 보내면 형식과 동의를 확인해 바꾸고, 안 보내면 저장된 번호를 유지한다. 첫 저장에는 필수다. */
  private void savePhoneNumber(Long userId, FestaPortfolioRequest request) {
    FestaInvestor existing = investorRepository.findByUserId(userId).orElse(null);
    String phoneNumber = request.phoneNumber() == null ? "" : request.phoneNumber().trim();

    if (phoneNumber.isEmpty()) {
      if (existing == null || !existing.hasPhoneNumber()) {
        throw new BusinessException(FestaErrorCode.PHONE_NUMBER_REQUIRED);
      }
      return;
    }
    if (!PHONE_NUMBER.matcher(phoneNumber).matches()) {
      throw new BusinessException(FestaErrorCode.INVALID_PHONE_NUMBER);
    }
    if (!Boolean.TRUE.equals(request.privacyConsent())) {
      throw new BusinessException(FestaErrorCode.PRIVACY_CONSENT_REQUIRED);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (existing == null) {
      investorRepository.save(FestaInvestor.of(userId, phoneNumber, now));
    } else {
      existing.registerPhone(phoneNumber, now);
    }
  }

  private FestaPortfolioResult toResult(Long userId) {
    List<FestaPortfolioResult.Investment> investments =
        investmentRepository.findByUserIdOrderByProjectId(userId).stream()
            .map(investment -> new FestaPortfolioResult.Investment(
                investment.getProjectId(), investment.getAmount()))
            .toList();
    long invested = investments.stream().mapToLong(FestaPortfolioResult.Investment::amount).sum();
    String masked = investorRepository.findByUserId(userId)
        .filter(FestaInvestor::hasPhoneNumber)
        .map(investor -> mask(investor.getPhoneNumber()))
        .orElse(null);
    return new FestaPortfolioResult(BUDGET, UNIT, BUDGET - invested, masked, investments, ownProjectIds(userId));
  }

  private List<Long> ownProjectIds(Long userId) {
    Long myGroupId = findMyGroupId(userId);
    if (myGroupId == null) {
      return List.of();
    }
    return projectQueryService.findFestaEntries().stream()
        .filter(project -> myGroupId.equals(project.groupId()))
        .map(ProjectView::id)
        .toList();
  }

  private Long findMyGroupId(Long userId) {
    return memberGroupService.findMyGroup(userId).map(GroupWithMembersResult::id).orElse(null);
  }

  /** 010-1234-5678 → 010-****-5678 */
  static String mask(String phoneNumber) {
    return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(8);
  }
}
