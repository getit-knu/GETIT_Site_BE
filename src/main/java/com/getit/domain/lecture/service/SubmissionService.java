package com.getit.domain.lecture.service;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.lecture.dto.SubmissionRequest;
import com.getit.domain.lecture.dto.SubmissionResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AllowedLinkHost;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionService {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
  private static final String DUPLICATE_SUBMISSION_CONSTRAINT =
      "uk_assignment_submission_assignment_id_user_id";

  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final AssignmentRepository assignmentRepository;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  @Transactional
  public SubmissionResult.Detail submit(Long assignmentId, SubmissionRequest.Submit request, Long userId) {
    Assignment assignment = findAssignment(assignmentId);

    if (assignmentSubmissionRepository.findByAssignmentIdAndUserId(assignmentId, userId).isPresent()) {
      throw new BusinessException(LectureErrorCode.DUPLICATE_SUBMISSION);
    }

    validateSubmissionContent(request.fileId(), request.linkUrl(), assignment);

    if (request.fileId() != null) {
      fileConnectionService.connectAll(List.of(request.fileId()));
    }

    LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
    SubmissionStatus status = determineStatus(now, assignment.getDeadline());

    AssignmentSubmission submission = AssignmentSubmission.submit(
        assignmentId, userId, request.fileId(), request.linkUrl(), request.comment(), status, now);
    try {
      assignmentSubmissionRepository.saveAndFlush(submission);
    } catch (DataIntegrityViolationException e) {
      if (!isDuplicateSubmissionConstraintViolation(e)) {
        throw e;
      }
      throw new BusinessException(LectureErrorCode.DUPLICATE_SUBMISSION);
    }

    return toResult(submission);
  }

  @Transactional
  public SubmissionResult.Detail resubmit(Long submissionId, SubmissionRequest.Submit request, Long userId) {
    AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));
    if (!submission.isOwnedBy(userId)) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    Assignment assignment = findAssignment(submission.getAssignmentId());
    validateSubmissionContent(request.fileId(), request.linkUrl(), assignment);

    swapConnectedFile(submission.getFileId(), request.fileId());

    LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
    SubmissionStatus status = determineStatus(now, assignment.getDeadline());
    submission.resubmit(request.fileId(), request.linkUrl(), request.comment(), status, now);

    return toResult(submission);
  }

  private boolean isDuplicateSubmissionConstraintViolation(DataIntegrityViolationException e) {
    Throwable cause = e.getMostSpecificCause();
    return cause.getMessage() != null && cause.getMessage().contains(DUPLICATE_SUBMISSION_CONSTRAINT);
  }

  private Assignment findAssignment(Long assignmentId) {
    return assignmentRepository.findById(assignmentId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ASSIGNMENT_NOT_FOUND));
  }

  private void validateSubmissionContent(Long fileId, String linkUrl, Assignment assignment) {
    if (fileId == null && linkUrl == null) {
      throw new BusinessException(LectureErrorCode.SUBMISSION_CONTENT_REQUIRED);
    }
    if (fileId != null && !assignment.getAllowedTypes().contains(SubmissionType.FILE)) {
      throw new BusinessException(LectureErrorCode.SUBMISSION_TYPE_NOT_ALLOWED);
    }
    if (linkUrl != null) {
      if (!assignment.getAllowedTypes().contains(SubmissionType.LINK)) {
        throw new BusinessException(LectureErrorCode.SUBMISSION_TYPE_NOT_ALLOWED);
      }
      validateLinkUrl(linkUrl);
    }
  }

  private void validateLinkUrl(String linkUrl) {
    String host;
    try {
      URI uri = new URI(linkUrl);
      if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
        throw new BusinessException(LectureErrorCode.INVALID_LINK_FORMAT);
      }
      host = uri.getHost();
    } catch (URISyntaxException e) {
      throw new BusinessException(LectureErrorCode.INVALID_LINK_FORMAT);
    }
    if (host == null || !AllowedLinkHost.isAllowed(host)) {
      throw new BusinessException(LectureErrorCode.LINK_HOST_NOT_ALLOWED);
    }
  }

  private void swapConnectedFile(Long oldFileId, Long newFileId) {
    if (oldFileId != null && !oldFileId.equals(newFileId)) {
      fileConnectionService.disconnectAll(List.of(oldFileId));
    }
    if (newFileId != null && !newFileId.equals(oldFileId)) {
      fileConnectionService.connectAll(List.of(newFileId));
    }
  }

  private SubmissionStatus determineStatus(LocalDateTime submittedAt, LocalDateTime deadline) {
    return submittedAt.isAfter(deadline) ? SubmissionStatus.LATE : SubmissionStatus.SUBMITTED;
  }

  private SubmissionResult.Detail toResult(AssignmentSubmission submission) {
    if (submission.getFileId() == null) {
      return SubmissionResult.Detail.of(submission, null, null);
    }
    FileInfo fileInfo = fileQueryService.findAllByIds(List.of(submission.getFileId())).stream()
        .filter(info -> Objects.equals(info.fileId(), submission.getFileId()))
        .findFirst()
        .orElse(null);
    if (fileInfo == null) {
      log.warn("과제 제출 파일 조회 실패. submissionId={}, fileId={}",
          submission.getId(), submission.getFileId());
      return SubmissionResult.Detail.of(submission, null, null);
    }
    return SubmissionResult.Detail.of(submission, fileInfo.url(), fileInfo.originalName());
  }
}
