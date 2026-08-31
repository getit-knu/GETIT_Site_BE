package com.getit.domain.lecture.admin.service;

import com.getit.domain.lecture.admin.dto.FeedbackRequest;
import com.getit.domain.lecture.admin.dto.FeedbackResult;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

  private final FeedbackRepository feedbackRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final UserAccountService userAccountService;

  @Transactional
  public FeedbackResult.CreateResult create(Long submissionId, FeedbackRequest.Write request, Long adminId) {
    AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));

    Feedback feedback = Feedback.create(submission.getId(), adminId, request.content());
    feedbackRepository.save(feedback);

    String adminName = findAdminName(adminId);
    return FeedbackResult.CreateResult.of(feedback, adminName);
  }

  @Transactional
  public FeedbackResult.UpdateResult update(Long feedbackId, FeedbackRequest.Write request, Long adminId) {
    Feedback feedback = feedbackRepository.findById(feedbackId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.FEEDBACK_NOT_FOUND));
    if (!feedback.isWrittenBy(adminId)) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    feedback.update(request.content());
    feedbackRepository.flush();
    return FeedbackResult.UpdateResult.from(feedback);
  }

  /**
   * 피드백 삭제. (이슈 #91 — PR #87 리뷰에서 지적)
   *
   * <p>작성자만 지울 수 있다. 수정({@link #update})과 같은 규칙이다. 다른 운영진이
   * 남의 피드백을 말없이 없앨 수 있으면 부원이 받은 피드백이 사라진 이유를 알 길이 없다.
   *
   * <p>soft delete 하지 않는다. Feedback 은 지우고 다시 쓰는 성격이고, 남겨 두면
   * 제출 목록의 feedbackDone 계산에서 매번 걸러내야 한다. 감사 기록이 필요하다면
   * 엔티티를 SoftDeletableEntity 로 바꾸는 별도 결정이 있어야 한다.
   */
  @Transactional
  public void delete(Long feedbackId, Long adminId) {
    Feedback feedback = feedbackRepository.findById(feedbackId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.FEEDBACK_NOT_FOUND));
    if (!feedback.isWrittenBy(adminId)) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    feedbackRepository.delete(feedback);
  }

  private String findAdminName(Long adminId) {
    return userAccountService.findActiveById(adminId)
        .map(UserAccount::name)
        .orElse("UNKNOWN");
  }
}
