package com.getit.domain.setting.photo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.TestStoredFiles;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.photo.dto.ActivityPhotoRequest;
import com.getit.domain.setting.photo.dto.ActivityPhotoResult;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.exception.ActivityPhotoErrorCode;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;
import com.getit.global.exception.BusinessException;

/**
 * 활동 사진 관리. (이슈 #146)
 *
 * <p>순서 규칙은 FAQ 와 같아서 같은 경계들을 확인한다. 여기에 더해 파일 연결 상태를 본다 —
 * 연결하지 않으면 미연결 파일 정리 배치가 24시간 뒤 사진을 지워버린다.
 */
@SpringBootTest
@Transactional
class ActivityPhotoAdminServiceTest {

  @Autowired
  private ActivityPhotoAdminService activityPhotoAdminService;

  @Autowired
  private ActivityPhotoRepository activityPhotoRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FileStorage fileStorage;

  private Long fileId;

  @BeforeEach
  void setUp() {
    fileId = uploadedFile("photo-1").getId();
  }

  private FileAsset uploadedFile(String key) {
    return TestStoredFiles.stored(fileAssetRepository, fileStorage,
        "public/" + key, key + ".png", "https://cdn/" + key, 1024L, "image/png", 1L);
  }

  private ActivityPhoto savedPhoto(int order) {
    return activityPhotoRepository.save(
        ActivityPhoto.create(uploadedFile("photo-" + order + "-" + order).getId(), order, true));
  }

  private List<Integer> orders() {
    return activityPhotoRepository.findAllByOrderByOrderAsc().stream()
        .map(ActivityPhoto::getOrder)
        .toList();
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("order 를 비우면 맨 뒤에 붙는다")
    void appendsWhenOrderOmitted() {
      savedPhoto(1);

      ActivityPhotoResult result =
          activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, null));

      assertThat(result.order()).isEqualTo(2);
      assertThat(result.imageUrl()).isNotBlank();
    }

    @Test
    @DisplayName("중간에 끼우면 뒤 항목이 한 칸씩 밀린다")
    void shiftsFollowingPhotos() {
      savedPhoto(1);
      savedPhoto(2);

      activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, 1));

      assertThat(orders()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("등록한 사진의 파일은 연결 상태가 된다")
    void connectsFile() {
      activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, null));

      // 연결하지 않으면 미연결 파일 정리 배치가 24시간 뒤 지워버린다.
      assertThat(fileAssetRepository.findById(fileId).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("삭제하면 뒤 항목이 당겨져 1..N 연속이 유지된다")
    void keepsOrderContinuous() {
      ActivityPhoto first = savedPhoto(1);
      savedPhoto(2);
      savedPhoto(3);

      activityPhotoAdminService.delete(first.getId());

      assertThat(orders()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("삭제하면 파일 연결이 해제된다")
    void disconnectsFile() {
      ActivityPhotoResult created =
          activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, null));

      activityPhotoAdminService.delete(created.id());

      // 해제해야 정리 배치가 실제 blob 까지 지워 용량이 남지 않는다.
      assertThat(fileAssetRepository.findById(fileId).orElseThrow().getStatus())
          .isEqualTo(FileStatus.PENDING);
    }

    @Test
    @DisplayName("없는 사진을 지우면 404")
    void notFound() {
      assertThatThrownBy(() -> activityPhotoAdminService.delete(999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ActivityPhotoErrorCode.ACTIVITY_PHOTO_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("사진을 교체하면 옛 파일은 연결이 풀리고 새 파일이 연결된다")
    void swapsFileConnection() {
      ActivityPhotoResult created =
          activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, null));
      Long newFileId = uploadedFile("photo-new").getId();

      activityPhotoAdminService.update(created.id(), new ActivityPhotoRequest(newFileId, true, null));

      assertThat(fileAssetRepository.findById(fileId).orElseThrow().getStatus())
          .isEqualTo(FileStatus.PENDING);
      assertThat(fileAssetRepository.findById(newFileId).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
      // 연결 쿼리가 영속성 컨텍스트를 비우므로, 순서를 잘못 두면 응답만 새 파일을 가리키고
      // DB 는 옛 파일을 계속 참조한다. 실제로 저장됐는지 본다 (PR #152 Copilot 리뷰 지적).
      assertThat(activityPhotoRepository.findById(created.id()).orElseThrow().getFileId())
          .isEqualTo(newFileId);
    }

    @Test
    @DisplayName("파일을 바꾸면서 순서도 함께 바꿀 수 있다")
    void changesFileAndOrderTogether() {
      ActivityPhotoResult first =
          activityPhotoAdminService.create(new ActivityPhotoRequest(fileId, true, null));
      savedPhoto(2);
      Long newFileId = uploadedFile("photo-swap").getId();

      activityPhotoAdminService.update(first.id(), new ActivityPhotoRequest(newFileId, false, 2));

      ActivityPhoto reloaded = activityPhotoRepository.findById(first.id()).orElseThrow();
      assertThat(reloaded.getFileId()).isEqualTo(newFileId);
      assertThat(reloaded.getOrder()).isEqualTo(2);
      assertThat(reloaded.isVisible()).isFalse();
      assertThat(orders()).containsExactly(1, 2);
    }

    @Test
    @DisplayName("비공개 저장소 파일은 붙일 수 없다")
    void rejectsPrivateFile() {
      FileAsset privateFile = TestStoredFiles.stored(fileAssetRepository, fileStorage,
          "private/lecture.pdf", "강의자료.pdf", "https://cdn/x", 10L, "application/pdf", 1L);

      // 붙이면 공개 홈이 5분짜리 서명 주소를 내려주게 되어 방문자에게 깨진 이미지가 된다.
      assertThatThrownBy(() -> activityPhotoAdminService.create(
          new ActivityPhotoRequest(privateFile.getId(), true, null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ActivityPhotoErrorCode.NOT_PUBLIC_FILE);
    }

    @Test
    @DisplayName("order 를 비우면 순서를 건드리지 않는다")
    void keepsOrderWhenOmitted() {
      ActivityPhoto photo = savedPhoto(1);
      savedPhoto(2);

      activityPhotoAdminService.update(
          photo.getId(), new ActivityPhotoRequest(photo.getFileId(), false, null));

      assertThat(orders()).containsExactly(1, 2);
      assertThat(activityPhotoRepository.findById(photo.getId()).orElseThrow().isVisible()).isFalse();
    }

    @Test
    @DisplayName("뒤로 옮기면 사이 항목이 앞으로 당겨진다")
    void movesBackward() {
      ActivityPhoto first = savedPhoto(1);
      savedPhoto(2);
      savedPhoto(3);

      activityPhotoAdminService.update(
          first.getId(), new ActivityPhotoRequest(first.getFileId(), true, 3));

      assertThat(activityPhotoRepository.findById(first.getId()).orElseThrow().getOrder())
          .isEqualTo(3);
      assertThat(orders()).containsExactly(1, 2, 3);
    }
  }
}
