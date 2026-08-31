package com.getit.domain.lecture.admin.service;

import com.getit.domain.lecture.admin.dto.FeedbackRequest;
import com.getit.domain.lecture.admin.dto.FeedbackResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.service.GenerationQueryService;
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
  private final AssignmentRepository assignmentRepository;
  private final LectureRepository lectureRepository;
  private final GenerationQueryService generationQueryService;
  private final UserAccountService userAccountService;

  @Transactional
  public FeedbackResult.CreateResult create(Long submissionId, FeedbackRequest.Write request, Long adminId) {
    AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));
    validateActiveGeneration(submission);

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
    validateActiveGeneration(findSubmission(feedback.getSubmissionId()));

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
    validateActiveGeneration(findSubmission(feedback.getSubmissionId()));

    feedbackRepository.delete(feedback);
  }

  /**
   * 지난 기수의 자료는 읽기 전용이다. (이슈 #168 — PR #167 리뷰에서 시작된 결정)
   *
   * <p>어드민은 지난 기수를 계속 열람한다. 그래서 없는 것처럼 404 로 감추지 않고,
   * 볼 수는 있지만 고칠 수 없다는 뜻으로 409 를 준다. 부원용 {@code SubmissionService} 가
   * 404 를 주는 것과 다른 이유다 — 부원에게는 애초에 보이지 않는 자료다.
   *
   * <p>작성 · 수정 · 삭제에 모두 건다. 하나만 막으면 고칠 수는 있는데 지울 수는 없는
   * 상태가 된다.
   */
  private void validateActiveGeneration(AssignmentSubmission submission) {
    Assignment assignment = assignmentRepository.findById(submission.getAssignmentId())
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ASSIGNMENT_NOT_FOUND));
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(assignment.getLectureId())
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
    Long activeGenerationId = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ACTIVE_GENERATION_NOT_FOUND))
        .id();

    if (!lecture.getGenerationId().equals(activeGenerationId)) {
      throw new BusinessException(LectureErrorCode.PAST_GENERATION_READ_ONLY);
    }
  }

  private AssignmentSubmission findSubmission(long submissionId) {
    return assignmentSubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));
  }

  private String findAdminName(Long adminId) {
    return userAccountService.findActiveById(adminId)
        .map(UserAccount::name)
        .orElse("UNKNOWN");
  }
}
