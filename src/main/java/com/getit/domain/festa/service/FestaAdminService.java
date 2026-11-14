package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaPhoneDestroyResult;
import com.getit.domain.festa.dto.FestaResetResult;
import com.getit.domain.festa.dto.FestaResultsResult;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaInvestmentRepository;
import com.getit.domain.festa.repository.FestaInvestmentRepository.InvestorRow;
import com.getit.domain.festa.repository.FestaInvestmentRepository.ProjectTotal;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.project.service.ProjectQueryService;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureQueryService;
import com.getit.domain.user.service.UserQueryService;
import com.getit.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 어드민이 보는 투자 집계와 추첨 명단. (이슈 #222) */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FestaAdminService {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  /** Excel 이 UTF-8 CSV 의 한글을 깨뜨리지 않게 한다. */
  private static final String BOM = "﻿";

  private final FestaInvestmentRepository investmentRepository;
  private final FestaInvestorRepository investorRepository;
  private final ProjectQueryService projectQueryService;
  private final UserQueryService userQueryService;
  private final FeatureQueryService featureQueryService;

  /** 출전 프로젝트 전부를 총액 내림차순으로. 투자가 없는 프로젝트도 0 으로 나온다. */
  public FestaResultsResult getResults() {
    Map<Long, ProjectTotal> totals = investmentRepository.sumByProject().stream()
        .collect(Collectors.toMap(ProjectTotal::projectId, Function.identity()));
    List<FestaResultsResult.ProjectResult> projects = projectQueryService.findFestaEntries().stream()
        .map(project -> {
          ProjectTotal total = totals.get(project.id());
          return new FestaResultsResult.ProjectResult(
              project.id(), project.title(), project.teamName(),
              total == null ? 0 : total.totalAmount(),
              total == null ? 0 : total.investorCount());
        })
        .sorted(Comparator.comparingLong(FestaResultsResult.ProjectResult::totalAmount).reversed())
        .toList();
    return new FestaResultsResult(investmentRepository.countInvestors(), projects);
  }

  /** 전체 번호가 들어간다. 누가 몇 건을 내려받았는지만 로그로 남기고 번호는 남기지 않는다. */
  public byte[] exportInvestorsCsv(Long adminId) {
    List<InvestorRow> rows = investmentRepository.findInvestorRows();
    Map<Long, String> names = userQueryService.findNamesByIds(
        rows.stream().map(InvestorRow::userId).toList());

    StringBuilder csv = new StringBuilder(BOM).append("이름,전화번호,마지막 투자 시각\r\n");
    for (InvestorRow row : rows) {
      csv.append(cell(names.getOrDefault(row.userId(), ""))).append(',')
          .append(cell(row.phoneNumber())).append(',')
          .append(cell(TIME.format(row.lastInvestedAt()))).append("\r\n");
    }
    log.info("FESTA 투자자 명단 내려받기. adminId={} rows={}", adminId, rows.size());
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  /** 번호와 동의 기록만 지운다. 투자 내역과 집계는 남는다. */
  @Transactional
  public FestaPhoneDestroyResult destroyPhoneNumbers(Long adminId) {
    int destroyed = investorRepository.destroyAllPhoneNumbers();
    log.info("FESTA 전화번호 파기. adminId={} count={}", adminId, destroyed);
    return new FestaPhoneDestroyResult(destroyed);
  }

  /**
   * 투자 내역과 전화번호를 모두 지운다. 리허설 데이터를 행사 전에 비우는 용도다. (이슈 #229)
   *
   * <p>토글이 켜져 있으면 거부한다 — 행사 중 실수로 누르는 것을 막는다. 서버는 행사 전인지 후인지
   * 구분하지 못한다. 결과 발표 전에 지우면 안 된다는 것은 화면 안내가 지킨다.
   */
  @Transactional
  public FestaResetResult resetAll(Long adminId) {
    boolean open = featureQueryService.findAll().stream()
        .anyMatch(feature -> feature.key() == FeatureKey.MOCK_INVESTMENT && feature.enabled());
    if (open) {
      throw new BusinessException(FestaErrorCode.FESTA_RESET_WHILE_OPEN);
    }
    long investorCount = investorRepository.count();
    investmentRepository.deleteAllInBatch();
    investorRepository.deleteAllInBatch();
    log.warn("FESTA 투자 데이터 초기화. adminId={} deletedInvestorCount={}", adminId, investorCount);
    return new FestaResetResult(investorCount);
  }

  /** 따옴표로 감싸고, 수식으로 읽히는 첫 글자(= + - @)는 앞에 작은따옴표를 붙여 무력화한다. */
  private static String cell(String value) {
    String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    return '"' + safe.replace("\"", "\"\"") + '"';
  }
}
