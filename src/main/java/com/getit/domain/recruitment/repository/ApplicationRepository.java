package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  /** 특정 기수에 낸 내 지원서 조회. (3.2) 사용자당 기수별 최대 1건이다. */
  Optional<Application> findByUserIdAndGenerationId(Long userId, Long generationId);

  /** 관리자 지원자 목록(7.1) - status 필터가 있을 때 조회한다. */
  Page<Application> findByGenerationIdAndStatus(Long generationId, ApplicationStatus status, Pageable pageable);

  /**
   * 관리자 지원자 목록(7.1) - status 필터가 없을 때 조회한다. 임시 저장만 하고 제출하지 않은
   * (DRAFT) 지원서는 심사 대상이 아니므로 제외한다.
   */
  Page<Application> findByGenerationIdAndStatusNot(Long generationId, ApplicationStatus status, Pageable pageable);

  /**
   * 7.4 합불 처리를 원자적으로 반영한다. {@code requiredStatus} 인 행만 갱신되고, 영향받은
   * 행 수로 갱신 성공 여부를 판단한다 (PR #52 Copilot 리뷰 지적 — "SUBMITTED 확인 후 갱신"을
   * 두 단계로 나누면, 동시에 들어온 두 결정 요청이 둘 다 확인을 통과해서 서로 다른 결과로 덮어쓸
   * 수 있었다. WHERE 절에 현재 상태를 함께 걸어두면 두 번째 요청은 반드시 0행을 갱신하게 된다).
   */
  @Modifying(clearAutomatically = true)
  @Query("update Application a set a.status = :newStatus where a.id = :id and a.status = :requiredStatus")
  int updateStatusIfCurrentStatus(
      @Param("id") Long id,
      @Param("newStatus") ApplicationStatus newStatus,
      @Param("requiredStatus") ApplicationStatus requiredStatus);

  /**
   * 엑셀 다운로드(7.6) - 7.1 과 동일한 필터를 페이징 없이 전체 조회한다. status 필터가 있을 때 쓴다.
   */
  List<Application> findByGenerationIdAndStatus(Long generationId, ApplicationStatus status, Sort sort);

  /**
   * 엑셀 다운로드(7.6) - status 필터가 없을 때 쓴다. 7.1 과 동일하게 DRAFT 는 제외한다.
   */
  List<Application> findByGenerationIdAndStatusNot(Long generationId, ApplicationStatus status, Sort sort);

  /**
   * 순차탐색(7.5) "다음" - 정렬 기준(제출일시 내림차순 + id 보조 정렬)에서 현재 위치
   * (submittedAt, id) 바로 다음 행 하나를 커서로 찾는다. 전체를 불러와 인덱스를 세는 대신
   * 바로 다음 한 건만 조회한다 (PR #54 리뷰 지적 — 지원자가 수백 명이면 상세 화면을 넘길 때마다
   * 전체 로딩 비용이 들었다). status 필터가 있을 때 쓴다. {@code pageable} 은 결과 1건만 받는 데
   * 쓴다({@code PageRequest.of(0, 1)}).
   */
  @Query("select a.id from Application a where a.generationId = :generationId and a.status = :status "
      + "and (a.submittedAt < :submittedAt or (a.submittedAt = :submittedAt and a.id < :id)) "
      + "order by a.submittedAt desc, a.id desc")
  List<Long> findNextIdByGenerationIdAndStatus(
      @Param("generationId") Long generationId,
      @Param("status") ApplicationStatus status,
      @Param("submittedAt") LocalDateTime submittedAt,
      @Param("id") Long id,
      Pageable pageable);

  /** 순차탐색(7.5) "다음" - status 필터가 없을 때 쓴다. 7.1 과 동일하게 DRAFT 는 제외한다. */
  @Query("select a.id from Application a where a.generationId = :generationId and a.status <> :excludedStatus "
      + "and (a.submittedAt < :submittedAt or (a.submittedAt = :submittedAt and a.id < :id)) "
      + "order by a.submittedAt desc, a.id desc")
  List<Long> findNextIdByGenerationIdAndStatusNot(
      @Param("generationId") Long generationId,
      @Param("excludedStatus") ApplicationStatus excludedStatus,
      @Param("submittedAt") LocalDateTime submittedAt,
      @Param("id") Long id,
      Pageable pageable);

  /** 순차탐색(7.5) "이전" - status 필터가 있을 때 쓴다. "다음"과 방향만 반대다. */
  @Query("select a.id from Application a where a.generationId = :generationId and a.status = :status "
      + "and (a.submittedAt > :submittedAt or (a.submittedAt = :submittedAt and a.id > :id)) "
      + "order by a.submittedAt asc, a.id asc")
  List<Long> findPreviousIdByGenerationIdAndStatus(
      @Param("generationId") Long generationId,
      @Param("status") ApplicationStatus status,
      @Param("submittedAt") LocalDateTime submittedAt,
      @Param("id") Long id,
      Pageable pageable);

  /** 순차탐색(7.5) "이전" - status 필터가 없을 때 쓴다. */
  @Query("select a.id from Application a where a.generationId = :generationId and a.status <> :excludedStatus "
      + "and (a.submittedAt > :submittedAt or (a.submittedAt = :submittedAt and a.id > :id)) "
      + "order by a.submittedAt asc, a.id asc")
  List<Long> findPreviousIdByGenerationIdAndStatusNot(
      @Param("generationId") Long generationId,
      @Param("excludedStatus") ApplicationStatus excludedStatus,
      @Param("submittedAt") LocalDateTime submittedAt,
      @Param("id") Long id,
      Pageable pageable);
}
