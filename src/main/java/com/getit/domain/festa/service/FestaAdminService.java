package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaDrawRequest;
import com.getit.domain.festa.dto.FestaPhoneDestroyResult;
import com.getit.domain.festa.dto.FestaResetResult;
import com.getit.domain.festa.dto.FestaResultsResult;
import com.getit.domain.festa.dto.FestaVoteResultsResult;
import com.getit.domain.festa.dto.FestaWinnersResult;
import com.getit.domain.festa.config.FestaProperties;
import com.getit.domain.festa.entity.FestaAuditAction;
import com.getit.domain.festa.entity.FestaAuditLog;
import com.getit.domain.festa.entity.FestaInvestor;
import com.getit.domain.festa.entity.FestaSchedule;
import com.getit.domain.festa.entity.FestaWinner;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaAuditLogRepository;
import com.getit.domain.festa.repository.FestaInvestmentRepository;
import com.getit.domain.festa.repository.FestaInvestmentRepository.InvestorRow;
import com.getit.domain.festa.repository.FestaInvestmentRepository.ProjectTotal;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.festa.repository.FestaScheduleRepository;
import com.getit.domain.festa.repository.FestaVoteRepository;
import com.getit.domain.festa.repository.FestaVoteRepository.ProjectCount;
import com.getit.domain.festa.repository.FestaVoteRepository.VoterRow;
import com.getit.domain.festa.repository.FestaWinnerRepository;
import com.getit.domain.project.service.ProjectQueryService;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureQueryService;
import com.getit.domain.user.service.UserQueryService;
import com.getit.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  /** 추첨 순서를 예측할 수 없게 한다. */
  private static final SecureRandom RANDOM = new SecureRandom();

  private final FestaInvestmentRepository investmentRepository;
  private final FestaInvestorRepository investorRepository;
  private final FestaVoteRepository voteRepository;
  private final ProjectQueryService projectQueryService;
  private final UserQueryService userQueryService;
  private final FeatureQueryService featureQueryService;
  private final FestaScheduleRepository scheduleRepository;
  private final FestaAuditLogRepository auditLogRepository;
  private final FestaWinnerRepository winnerRepository;
  private final FestaProperties festaProperties;
  private final Clock clock;

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

  /** 출전 프로젝트 전부를 득표 내림차순으로. 표가 없는 프로젝트도 0 으로 나온다. (이슈 #232) */
  public FestaVoteResultsResult getVoteResults() {
    Map<Long, Long> counts = voteRepository.countByProject().stream()
        .collect(Collectors.toMap(ProjectCount::projectId, ProjectCount::voteCount));
    List<FestaVoteResultsResult.ProjectResult> projects = projectQueryService.findFestaEntries().stream()
        .map(project -> new FestaVoteResultsResult.ProjectResult(
            project.id(), project.title(), project.teamName(), counts.getOrDefault(project.id(), 0L)))
        .sorted(Comparator.comparingLong(FestaVoteResultsResult.ProjectResult::voteCount).reversed())
        .toList();
    return new FestaVoteResultsResult(voteRepository.count(), projects);
  }

  /**
   * 참여자 명단. 번호는 가려서 넣는다 — 추첨은 서버가 하므로({@link #draw}) 전체 번호가 담긴
   * 파일이 PC 나 메신저에 남을 일을 만들지 않는다. (이슈 #238) 투표가 끝난 뒤에만 내려받을 수 있고,
   * 누가 몇 건을 받았는지 DB 에 남긴다.
   */
  @Transactional
  public byte[] exportInvestorsCsv(Long adminId) {
    ensureVotingEnded(FestaErrorCode.FESTA_EXPORT_BEFORE_END);
    Map<Long, ParticipantRow> byUser = new HashMap<>();
    for (InvestorRow row : investmentRepository.findInvestorRows()) {
      byUser.merge(row.userId(), new ParticipantRow(row.userId(), row.phoneNumber(), row.lastInvestedAt()),
          FestaAdminService::later);
    }
    for (VoterRow row : voteRepository.findVoterRows()) {
      byUser.merge(row.userId(), new ParticipantRow(row.userId(), row.phoneNumber(), row.votedAt()),
          FestaAdminService::later);
    }
    List<ParticipantRow> rows = byUser.values().stream()
        .sorted(Comparator.comparing(ParticipantRow::lastAt).reversed())
        .toList();
    Map<Long, String> names = userQueryService.findNamesByIds(
        rows.stream().map(ParticipantRow::userId).toList());

    StringBuilder csv = new StringBuilder(BOM).append("이름,전화번호,마지막 참여 시각\r\n");
    for (ParticipantRow row : rows) {
      csv.append(cell(names.getOrDefault(row.userId(), ""))).append(',')
          .append(cell(row.phoneNumber() == null ? "" : FestaParticipation.mask(row.phoneNumber()))).append(',')
          .append(cell(TIME.format(row.lastAt()))).append("\r\n");
    }
    audit(FestaAuditAction.EXPORT_PARTICIPANTS, adminId, rows.size());
    log.info("FESTA 참여자 명단 내려받기. adminId={} rows={}", adminId, rows.size());
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  private static ParticipantRow later(ParticipantRow a, ParticipantRow b) {
    return a.lastAt().isAfter(b.lastAt()) ? a : b;
  }

  /** 투자나 투표로 참여한 사람과 그 마지막 시각. 한 사람이 둘 다 했으면 늦은 쪽을 쓴다. */
  private record ParticipantRow(Long userId, String phoneNumber, LocalDateTime lastAt) { }

  /**
   * 번호를 낸 참여자 중에서 무작위로 뽑아 저장한다. 투표가 끝난 뒤에만 할 수 있다. (이슈 #238)
   *
   * <p>이미 뽑은 결과는 덮어쓰지 않는다. {@code additional} 이 아니면 거부하고, 맞으면 이미 뽑힌
   * 사람을 빼고 순번을 이어서 더 뽑는다. 후보가 요청 인원보다 적으면 후보 전원이 뽑힌다.
   */
  @Transactional
  public FestaWinnersResult draw(Long adminId, FestaDrawRequest request) {
    ensureVotingEnded(FestaErrorCode.FESTA_DRAW_BEFORE_END);
    List<FestaWinner> existing = winnerRepository.findAllByOrderByDrawOrderAsc();
    if (!existing.isEmpty() && !request.isAdditional()) {
      throw new BusinessException(FestaErrorCode.FESTA_ALREADY_DRAWN);
    }
    Set<Long> alreadyWon = existing.stream().map(FestaWinner::getUserId).collect(Collectors.toSet());
    List<Long> candidates = new ArrayList<>(investorRepository.findUserIdsWithPhoneNumber().stream()
        .filter(userId -> !alreadyWon.contains(userId))
        .toList());
    if (candidates.isEmpty()) {
      throw new BusinessException(FestaErrorCode.FESTA_NO_DRAW_CANDIDATES);
    }
    Collections.shuffle(candidates, RANDOM);

    int picked = Math.min(request.countOrDefault(), candidates.size());
    LocalDateTime now = LocalDateTime.now(clock);
    List<FestaWinner> winners = new ArrayList<>(existing);
    for (int i = 0; i < picked; i++) {
      winners.add(winnerRepository.save(FestaWinner.of(candidates.get(i), existing.size() + i + 1, adminId, now)));
    }
    audit(FestaAuditAction.DRAW_WINNERS, adminId, picked);
    log.info("FESTA 경품 추첨. adminId={} picked={} candidates={} additional={}",
        adminId, picked, candidates.size(), request.isAdditional());
    return toWinnersResult(winners);
  }

  /** 당첨자와 전체 번호. 볼 때마다 기록한다. 아직 안 뽑았으면 빈 목록. (이슈 #238) */
  @Transactional
  public FestaWinnersResult getWinners(Long adminId) {
    List<FestaWinner> winners = winnerRepository.findAllByOrderByDrawOrderAsc();
    if (!winners.isEmpty()) {
      audit(FestaAuditAction.VIEW_WINNERS, adminId, winners.size());
    }
    return toWinnersResult(winners);
  }

  private FestaWinnersResult toWinnersResult(List<FestaWinner> winners) {
    if (winners.isEmpty()) {
      return new FestaWinnersResult(List.of());
    }
    List<Long> userIds = winners.stream().map(FestaWinner::getUserId).toList();
    Map<Long, String> names = userQueryService.findNamesByIds(userIds);
    Map<Long, String> phones = new HashMap<>();
    for (FestaInvestor investor : investorRepository.findAllByUserIdIn(userIds)) {
      if (investor.hasPhoneNumber()) {
        phones.put(investor.getUserId(), investor.revealPhoneNumber());
      }
    }
    return new FestaWinnersResult(winners.stream()
        .map(winner -> new FestaWinnersResult.Winner(
            winner.getDrawOrder(), winner.getUserId(), names.getOrDefault(winner.getUserId(), ""),
            phones.get(winner.getUserId()),
            winner.getDrawnAt().atZone(clock.getZone()).toOffsetDateTime()))
        .toList());
  }

  /** 번호와 동의 기록만 지운다. 투자 내역과 집계는 남는다. */
  @Transactional
  public FestaPhoneDestroyResult destroyPhoneNumbers(Long adminId) {
    int destroyed = investorRepository.destroyAllPhoneNumbers();
    audit(FestaAuditAction.DESTROY_PHONES, adminId, destroyed);
    log.info("FESTA 전화번호 파기. adminId={} count={}", adminId, destroyed);
    return new FestaPhoneDestroyResult(destroyed);
  }

  /**
   * 투자 내역, 투표, 당첨자, 전화번호를 모두 지운다. 리허설 데이터를 행사 전에 비우는 용도다. (이슈 #229)
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
    voteRepository.deleteAllInBatch();
    winnerRepository.deleteAllInBatch();
    investorRepository.deleteAllInBatch();
    audit(FestaAuditAction.RESET_ALL, adminId, investorCount);
    log.warn("FESTA 투자 데이터 초기화. adminId={} deletedInvestorCount={}", adminId, investorCount);
    return new FestaResetResult(investorCount);
  }

  /**
   * 투표 종료 뒤 보관 기간이 지났으면 번호를 파기한다. 스케줄러가 매일 부른다.
   * 일정이 없으면 언제 끝났는지 몰라 건드리지 않는다.
   */
  @Transactional
  public void destroyPhoneNumbersIfExpired() {
    FestaSchedule schedule = scheduleRepository.findById(FestaSchedule.SINGLETON_ID).orElse(null);
    if (schedule == null) {
      return;
    }
    LocalDateTime deadline = schedule.getEndAt().plusDays(festaProperties.retentionDays());
    if (LocalDateTime.now(clock).isBefore(deadline) || !investorRepository.existsByPhoneNumberIsNotNull()) {
      return;
    }
    int destroyed = investorRepository.destroyAllPhoneNumbers();
    audit(FestaAuditAction.AUTO_DESTROY_PHONES, null, destroyed);
    log.info("FESTA 전화번호 자동 파기. count={} retentionDays={}", destroyed, festaProperties.retentionDays());
  }

  /** 일정이 있으면 종료 시각이 지나야 하고, 없으면 토글이 꺼져 있어야 한다. */
  private void ensureVotingEnded(FestaErrorCode notEnded) {
    boolean ended = scheduleRepository.findById(FestaSchedule.SINGLETON_ID)
        .map(schedule -> !LocalDateTime.now(clock).isBefore(schedule.getEndAt()))
        .orElseGet(() -> featureQueryService.findAll().stream()
            .noneMatch(feature -> feature.key() == FeatureKey.MOCK_INVESTMENT && feature.enabled()));
    if (!ended) {
      throw new BusinessException(notEnded);
    }
  }

  private void audit(FestaAuditAction action, Long adminId, long count) {
    auditLogRepository.save(FestaAuditLog.of(action, adminId, count, LocalDateTime.now(clock)));
  }

  /** 따옴표로 감싸고, 수식으로 읽히는 첫 글자(= + - @)는 앞에 작은따옴표를 붙여 무력화한다. */
  private static String cell(String value) {
    String safe = !value.isEmpty() && "=+-@\t\r".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
    return '"' + safe.replace("\"", "\"\"") + '"';
  }
}
