package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaInvestment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FestaInvestmentRepository extends JpaRepository<FestaInvestment, Long> {

  List<FestaInvestment> findByUserIdOrderByProjectId(Long userId);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from FestaInvestment i where i.userId = :userId")
  void deleteByUserId(@Param("userId") Long userId);

  /** 프로젝트별 총액과 투자자 수. 투자 내역이 없는 프로젝트는 나오지 않는다. */
  @Query("""
      select new com.getit.domain.festa.repository.FestaInvestmentRepository$ProjectTotal(
          i.projectId, sum(i.amount), count(i))
      from FestaInvestment i
      group by i.projectId
      """)
  List<ProjectTotal> sumByProject();

  @Query("select count(distinct i.userId) from FestaInvestment i")
  long countInvestors();

  /** 투자 내역이 남아 있고 번호가 있는 사람의 마지막 투자 시각. 모두 0 으로 돌린 사람은 행이 없어 빠진다. */
  @Query("""
      select new com.getit.domain.festa.repository.FestaInvestmentRepository$InvestorRow(
          v.userId, v.phoneNumber, max(i.updatedAt))
      from FestaInvestment i, FestaInvestor v
      where v.userId = i.userId and v.phoneNumber is not null
      group by v.userId, v.phoneNumber
      order by max(i.updatedAt) desc
      """)
  List<InvestorRow> findInvestorRows();

  record ProjectTotal(Long projectId, Long totalAmount, Long investorCount) { }

  record InvestorRow(Long userId, String phoneNumber, LocalDateTime lastInvestedAt) { }
}
