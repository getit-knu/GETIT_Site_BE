package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.TestStoredFiles;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.lecture.dto.SubmissionRequest;
import com.getit.domain.lecture.dto.SubmissionResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SubmissionServiceTest {

  @Autowired
  private SubmissionService submissionService;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FileStorage fileStorage;

  private static final Long USER_ID = 100L;

  private Long fileAssignmentId;
  private Long linkAssignmentId;
  private Long bothAssignmentId;
  private Long activeLectureId;
  private LocalDateTime futureDeadline;

  @BeforeEach
  void setUp() {
    Long generationId = activeGenerationId();
    activeLectureId = createLecture(generationId);

    futureDeadline = LocalDateTime.now().plusDays(7);
    fileAssignmentId = assignmentRepository.save(Assignment.create(
        createLecture(generationId), "파일 과제", "설명", futureDeadline, Set.of(SubmissionType.FILE), null))
        .getId();
    linkAssignmentId = assignmentRepository.save(Assignment.create(
        createLecture(generationId), "링크 과제", "설명", futureDeadline, Set.of(SubmissionType.LINK), null))
        .getId();
    bothAssignmentId = assignmentRepository.save(Assignment.create(
        createLecture(generationId), "파일·링크 과제", "설명", futureDeadline,
        Set.of(SubmissionType.FILE, SubmissionType.LINK), null)).getId();
  }

  private Long activeGenerationId() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    return generationRepository.save(generation).getId();
  }

  private Long createLecture(Long generationId) {
    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
    return lectureRepository.save(Lecture.create(
        1, "테스트 강의", null, null, null, null, true, generationId, trackId, subCategoryId, 1L)).getId();
  }

  private Long uploadFile() {
    return uploadFileBy(USER_ID);
  }

  private Long uploadFileBy(Long uploaderId) {
    return TestStoredFiles.stored(
        fileAssetRepository, fileStorage,
        "key/1", "과제.zip", "https://cdn/key/1", 1024L, "application/zip", uploaderId).getId();
  }

  @Nested
  class Submit {

    @Test
    @DisplayName("파일로 제출하면 파일이 연결되고 SUBMITTED 상태가 된다")
    void submitsWithFile() {
      Long fileId = uploadFile();

      SubmissionResult.Detail result = submissionService.submit(
          fileAssignmentId, new SubmissionRequest.Submit(fileId, null, "제출합니다"), USER_ID);

      assertThat(result.status()).isEqualTo(SubmissionStatus.SUBMITTED);
      assertThat(result.fileName()).isEqualTo("과제.zip");
      assertThat(fileAssetRepository.findById(fileId)).get()
          .extracting(FileAsset::getStatus).isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("본인이 업로드하지 않은 파일이면 예외가 발생한다")
    void throwsWhenFileNotOwnedByRequester() {
      Long otherUsersFileId = uploadFileBy(999L);

      assertThatThrownBy(() -> submissionService.submit(
          fileAssignmentId, new SubmissionRequest.Submit(otherUsersFileId, null, null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    @Test
    @DisplayName("링크로 제출하면 LATE/SUBMITTED 상태가 정확히 판정된다")
    void submitsWithLinkAndMarksLateWhenPastDeadline() {
      Assignment pastDeadlineAssignment = assignmentRepository.save(Assignment.create(
          activeLectureId, "마감지남", "설명", LocalDateTime.now().minusDays(1),
          Set.of(SubmissionType.LINK), null));

      SubmissionResult.Detail result = submissionService.submit(
          pastDeadlineAssignment.getId(),
          new SubmissionRequest.Submit(null, "https://github.com/user/repo", null),
          USER_ID);

      assertThat(result.status()).isEqualTo(SubmissionStatus.LATE);
      assertThat(result.linkUrl()).isEqualTo("https://github.com/user/repo");
    }

    @Test
    @DisplayName("존재하지 않는 과제면 예외가 발생한다")
    void throwsWhenAssignmentNotFound() {
      assertThatThrownBy(() -> submissionService.submit(
          999_999L, new SubmissionRequest.Submit(null, "https://github.com/user/repo", null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.ASSIGNMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("과거 기수 강의의 과제면 예외가 발생한다")
    void throwsWhenLectureNotInActiveGeneration() {
      Generation pastGeneration = generationRepository.save(Generation.create(8, 2025));
      Long pastLectureId = createLecture(pastGeneration.getId());
      Assignment pastAssignment = assignmentRepository.save(Assignment.create(
          pastLectureId, "지난 기수 과제", "설명", futureDeadline, Set.of(SubmissionType.LINK), null));

      assertThatThrownBy(() -> submissionService.submit(
          pastAssignment.getId(), new SubmissionRequest.Submit(null, "https://github.com/user/repo", null),
          USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.ASSIGNMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 제출했으면 예외가 발생한다")
    void throwsWhenDuplicateSubmission() {
      submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(null, "https://github.com/user/repo", null), USER_ID);

      assertThatThrownBy(() -> submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(null, "https://gitlab.com/user/repo", null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.DUPLICATE_SUBMISSION);
    }

    @Test
    @DisplayName("파일도 링크도 없으면 예외가 발생한다")
    void throwsWhenContentMissing() {
      assertThatThrownBy(() -> submissionService.submit(
          bothAssignmentId, new SubmissionRequest.Submit(null, null, null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBMISSION_CONTENT_REQUIRED);
    }

    @Test
    @DisplayName("과제가 허용하지 않는 제출 방식이면 예외가 발생한다")
    void throwsWhenSubmissionTypeNotAllowed() {
      Long fileId = uploadFile();

      assertThatThrownBy(() -> submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(fileId, null, null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBMISSION_TYPE_NOT_ALLOWED);
    }

    @Test
    @DisplayName("링크 형식이 올바르지 않으면 예외가 발생한다")
    void throwsWhenLinkFormatInvalid() {
      assertThatThrownBy(() -> submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(null, "not-a-url", null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.INVALID_LINK_FORMAT);
    }

    @Test
    @DisplayName("화이트리스트에 없는 호스트면 예외가 발생한다")
    void throwsWhenLinkHostNotAllowed() {
      assertThatThrownBy(() -> submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(null, "https://evil.com/x", null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.LINK_HOST_NOT_ALLOWED);
    }
  }

  @Nested
  class Resubmit {

    @Test
    @DisplayName("파일에서 링크로 재제출하면 기존 파일 연결이 해제된다")
    void resubmitSwapsFileToLink() {
      Long fileId = uploadFile();
      SubmissionResult.Detail submitted = submissionService.submit(
          bothAssignmentId, new SubmissionRequest.Submit(fileId, null, null), USER_ID);
      AssignmentSubmission submission = assignmentSubmissionRepository.findById(submitted.id()).orElseThrow();

      SubmissionResult.Detail result = submissionService.resubmit(
          submission.getId(), new SubmissionRequest.Submit(null, "https://github.com/user/repo", "수정"), USER_ID);

      assertThat(result.linkUrl()).isEqualTo("https://github.com/user/repo");
      assertThat(result.fileUrl()).isNull();
      assertThat(fileAssetRepository.findById(fileId)).get()
          .extracting(FileAsset::getStatus).isEqualTo(FileStatus.PENDING);
    }

    @Test
    @DisplayName("본인 제출물이 아니면 예외가 발생한다")
    void throwsWhenNotOwner() {
      SubmissionResult.Detail submitted = submissionService.submit(
          linkAssignmentId, new SubmissionRequest.Submit(null, "https://github.com/user/repo", null), USER_ID);

      assertThatThrownBy(() -> submissionService.resubmit(
          submitted.id(), new SubmissionRequest.Submit(null, "https://gitlab.com/user/repo", null), 999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    @Test
    @DisplayName("존재하지 않는 제출물이면 예외가 발생한다")
    void throwsWhenSubmissionNotFound() {
      assertThatThrownBy(() -> submissionService.resubmit(
          999_999L, new SubmissionRequest.Submit(null, "https://github.com/user/repo", null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBMISSION_NOT_FOUND);
    }

    @Test
    @DisplayName("본인이 업로드하지 않은 파일로 재제출하면 예외가 발생한다")
    void throwsWhenResubmitFileNotOwnedByRequester() {
      SubmissionResult.Detail submitted = submissionService.submit(
          bothAssignmentId, new SubmissionRequest.Submit(null, "https://github.com/user/repo", null), USER_ID);
      Long otherUsersFileId = uploadFileBy(999L);

      assertThatThrownBy(() -> submissionService.resubmit(
          submitted.id(), new SubmissionRequest.Submit(otherUsersFileId, null, null), USER_ID))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
    }
  }
}
