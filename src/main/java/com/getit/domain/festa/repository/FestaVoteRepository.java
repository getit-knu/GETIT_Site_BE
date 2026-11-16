package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaVote;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FestaVoteRepository extends JpaRepository<FestaVote, Long> {

  Optional<FestaVote> findByUserId(Long userId);

  /** 프로젝트별 득표수. 표가 없는 프로젝트는 나오지 않는다. */
  @Query("""
      select new com.getit.domain.festa.repository.FestaVoteRepository$ProjectCount(v.projectId, count(v))
      from FestaVote v
      group by v.projectId
      """)
  List<ProjectCount> countByProject();

  /** 번호가 있는 투표자와 마지막 투표 시각. */
  @Query("""
      select new com.getit.domain.festa.repository.FestaVoteRepository$VoterRow(
          v.userId, i.phoneNumber, v.updatedAt)
      from FestaVote v, FestaInvestor i
      where i.userId = v.userId and i.phoneNumber is not null
      """)
  List<VoterRow> findVoterRows();

  record ProjectCount(Long projectId, Long voteCount) { }

  record VoterRow(Long userId, String phoneNumber, LocalDateTime votedAt) { }
}
