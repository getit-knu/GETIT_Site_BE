package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaVoteRequest;
import com.getit.domain.festa.dto.FestaVoteResult;
import com.getit.domain.festa.entity.FestaVote;
import com.getit.domain.festa.repository.FestaVoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 모의투자 FESTA 1인 1팀 투표. (이슈 #232)
 *
 * <p>한 사람은 한 팀에만 투표한다. 행사 기간 안에서는 다시 보내 팀을 바꿀 수 있다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FestaVoteService {

  private final FestaVoteRepository voteRepository;
  private final FestaParticipation participation;

  /** 토글이 꺼져 있어도 내 현황은 볼 수 있다. 고칠 수만 없다. */
  @Transactional(readOnly = true)
  public FestaVoteResult getVote(Long userId) {
    return toResult(userId);
  }

  public FestaVoteResult vote(Long userId, FestaVoteRequest request) {
    participation.ensureOpen();
    participation.validateProject(userId, request.projectId());
    participation.savePhoneNumber(userId, request.phoneNumber(), request.privacyConsent());

    voteRepository.findByUserId(userId).ifPresentOrElse(
        vote -> vote.changeProject(request.projectId()),
        () -> voteRepository.save(FestaVote.of(userId, request.projectId())));
    voteRepository.flush();
    return toResult(userId);
  }

  private FestaVoteResult toResult(Long userId) {
    Long projectId = voteRepository.findByUserId(userId).map(FestaVote::getProjectId).orElse(null);
    return new FestaVoteResult(projectId, participation.maskedPhoneNumber(userId), participation.ownProjectIds(userId));
  }
}
