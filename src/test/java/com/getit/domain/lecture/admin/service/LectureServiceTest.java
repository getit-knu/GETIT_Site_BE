package com.getit.domain.lecture.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.lecture.admin.dto.LectureRequest;
import com.getit.domain.lecture.admin.dto.LectureRequest.AssignmentPart;
import com.getit.domain.lecture.admin.dto.LectureResult;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.LectureFile;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.LectureFileRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
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
class LectureServiceTest {

  @Autowired
  private LectureService lectureService;

  @Autowired
  private LectureFileRepository lectureFileRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private Long activeGenerationId;
  private Long trackId;
  private Long subCategoryId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();

    Track track = trackRepository.save(Track.create("SW", 1));
    trackId = track.getId();
    subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
  }

  private LectureRequest.Create createRequest(Long generationId, Long trackId, Long subCategoryId) {
    return new LectureRequest.Create(
        generationId, trackId, subCategoryId, 1, "HTML/CSS 기초", "## 학습 구성",
        "https://youtube.com/watch?v=abc123", "https://docs.getit.com/web-basic", 120,
        null, true, null);
  }

  @Nested
  class CreateLecture {

    @Test
    @DisplayName("생성 시 기수를 생략하면 활성 기수로 채워진다")
    void createsWithActiveGenerationWhenOmitted() {
      Lecture lecture = lectureService.createLecture(createRequest(null, trackId, subCategoryId), 100L);

      assertThat(lecture.getGenerationId()).isEqualTo(activeGenerationId);
      assertThat(lecture.getCreatedBy()).isEqualTo(100L);
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      generationRepository.findById(activeGenerationId).ifPresent(g -> {
        g.deactivate();
        generationRepository.save(g);
      });

      assertThatThrownBy(() -> lectureService.createLecture(createRequest(null, trackId, subCategoryId), 100L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 트랙이면 예외가 발생한다")
    void throwsWhenTrackNotFound() {
      assertThatThrownBy(() -> lectureService.createLecture(createRequest(null, 999_999L, null), 100L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.TRACK_NOT_FOUND);
    }

    @Test
    @DisplayName("소분류가 다른 트랙 소속이면 예외가 발생한다")
    void throwsWhenSubCategoryTrackMismatch() {
      Track otherTrack = trackRepository.save(Track.create("창업", 2));

      assertThatThrownBy(
          () -> lectureService.createLecture(createRequest(null, otherTrack.getId(), subCategoryId), 100L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBCATEGORY_TRACK_MISMATCH);
    }

    @Test
    @DisplayName("fileIds 를 연결하고 원본 파일명을 표시 이름으로 저장한다")
    void connectsFilesWithOriginalNameAsDisplayName() {
      FileAsset file = fileAssetRepository.save(
          FileAsset.upload("key/1", "1주차자료.pdf", "https://cdn/key/1", 1024L, "application/pdf", 100L));

      Lecture lecture = lectureService.createLecture(
          new LectureRequest.Create(
              null, trackId, subCategoryId, 1, "HTML/CSS 기초", null, null, null, null,
              List.of(file.getId()), true, null),
          100L);

      assertThat(lectureFileRepository.findAllByLectureIdOrderByIdAsc(lecture.getId()))
          .extracting("displayName", "fileId")
          .containsExactly(tuple("1주차자료.pdf", file.getId()));
      assertThat(fileAssetRepository.findById(file.getId())).get()
          .extracting(FileAsset::getStatus).isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("존재하지 않는 fileId 면 예외가 발생한다")
    void throwsWhenFileNotFound() {
      assertThatThrownBy(() -> lectureService.createLecture(
          new LectureRequest.Create(
              null, trackId, subCategoryId, 1, "HTML/CSS 기초", null, null, null, null,
              List.of(999_999L), true, null),
          100L))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("999999");
    }

    @Test
    @DisplayName("fileIds에 중복이 있어도 한 번만 연결한다")
    void deduplicatesFileIds() {
      FileAsset file = fileAssetRepository.save(
          FileAsset.upload("key/1", "1주차자료.pdf", "https://cdn/key/1", 1024L, "application/pdf", 100L));

      Lecture lecture = lectureService.createLecture(
          new LectureRequest.Create(
              null, trackId, subCategoryId, 1, "HTML/CSS 기초", null, null, null, null,
              List.of(file.getId(), file.getId()), true, null),
          100L);

      assertThat(lectureFileRepository.findAllByLectureIdOrderByIdAsc(lecture.getId())).hasSize(1);
    }

    @Test
    @DisplayName("과제를 함께 생성한다")
    void createsAssignmentTogether() {
      LocalDateTime deadline = LocalDateTime.of(2026, 6, 19, 23, 59, 59);
      AssignmentPart assignmentPart = new AssignmentPart(
          "자기소개 페이지 만들기", "HTML/CSS로 만들어보세요.", deadline,
          Set.of(SubmissionType.LINK), "GitHub 저장소 URL을 입력하세요");

      Lecture lecture = lectureService.createLecture(
          new LectureRequest.Create(
              null, trackId, subCategoryId, 1, "HTML/CSS 기초", null, null, null, null,
              null, true, assignmentPart),
          100L);

      LectureResult.DetailResult detail = lectureService.getLecture(lecture.getId());
      assertThat(detail.assignment()).isNotNull();
      assertThat(detail.assignment().deadline()).isEqualTo(deadline);
      assertThat(detail.assignment().allowedTypes()).containsExactly(SubmissionType.LINK);
      assertThat(detail.assignment().linkPlaceholder()).isEqualTo("GitHub 저장소 URL을 입력하세요");
    }
  }

  @Nested
  class GetLectures {

    @Test
    @DisplayName("트랙 id 로 필터링해 week 오름차순으로 반환한다")
    void filtersByTrackId() {
      Track otherTrack = trackRepository.save(Track.create("창업", 2));
      lectureService.createLecture(createRequest(null, trackId, subCategoryId), 100L);
      lectureService.createLecture(createRequest(null, otherTrack.getId(), null), 100L);

      LectureResult.ListResult result = lectureService.getLectures(activeGenerationId, trackId, null);

      assertThat(result.lectures()).extracting(LectureResult.LectureCard::title).containsExactly("HTML/CSS 기초");
      assertThat(result.tracks()).hasSize(2);
    }
  }

  @Nested
  class GetLecture {

    @Test
    @DisplayName("없는 강의 id 면 예외가 발생한다")
    void throwsWhenLectureNotFound() {
      assertThatThrownBy(() -> lectureService.getLecture(999_999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("연결된 파일 조회 실패 시 목록에서 제외하고 예외를 발생시키지 않는다")
    void excludesFileWhenFileNotFound() {
      Lecture lecture = lectureService.createLecture(createRequest(null, trackId, subCategoryId), 100L);
      lectureFileRepository.save(LectureFile.create("삭제된파일.pdf", lecture.getId(), 999_999L));

      LectureResult.DetailResult detail = lectureService.getLecture(lecture.getId());

      assertThat(detail.files()).isEmpty();
    }
  }
}
