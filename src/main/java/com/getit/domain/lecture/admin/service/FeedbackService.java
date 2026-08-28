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

  private String findAdminName(Long adminId) {
    return userAccountService.findActiveById(adminId)
        .map(UserAccount::name)
        .orElse("UNKNOWN");
  }
}
