package com.getit.domain.project.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.TestStoredFiles;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.admin.dto.ProjectResult;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.exception.ProjectErrorCode;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProjectAdminServiceTest {

  @Autowired
  private ProjectAdminService projectAdminService;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FileStorage fileStorage;

  private ProjectRequest.Write write(String title, String semester, Long fileId, Integer order) {
    return new ProjectRequest.Write(title, "팀", semester, "설명",
        List.of("React"), "https://code", "https://demo", fileId, false, order, null, null);
  }

  private Project seed(String title, String semester, int order) {
    ProjectCommand command = new ProjectCommand(
        title, "팀", semester, null, List.of(), null, null, false, null);
    return projectRepository.save(Project.create(command, order));
  }

  @Nested
  @DisplayName("getProjects")
  class GetProjects {

    @Test
    @DisplayName("학기로 거르고 order 순으로 반환한다")
    void filtersBySemesterOrderedByOrder() {
      seed("B", "2025-FALL", 2);
      seed("A", "2025-FALL", 1);
      seed("C", "2024-SPRING", 1);

      var result = projectAdminService.getProjects("2025-FALL", null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ProjectResult.Item::title).containsExactly("A", "B");
    }

    @Test
    @DisplayName("fileId 가 있으면 thumbnailUrl 을 해석한다")
    void resolvesThumbnailUrl() {
      FileAsset file = TestStoredFiles.stored(fileAssetRepository, fileStorage,
          "admin-thumb-key", "thumb.png", "https://cdn/thumb.png", 10L, "image/png", 1L);
      projectAdminService.createProject(write("썸네일", "2025-FALL", file.getId(), 1));

      var result = projectAdminService.getProjects("2025-FALL", null, PageRequest.of(0, 20));

      assertThat(result.content().get(0).thumbnailUrl()).isEqualTo("http://localhost:8080/api/public/files/admin-thumb-key");
    }

    @Test
    @DisplayName("fileId 가 없으면 thumbnailUrl 은 null")
    void nullThumbnailWhenNoFile() {
      seed("파일없음", "2025-FALL", 1);

      var result = projectAdminService.getProjects("2025-FALL", null, PageRequest.of(0, 20));

      assertThat(result.content().get(0).thumbnailUrl()).isNull();
    }
  }

  @Nested
  @DisplayName("createProject")
  class CreateProject {

    @Test
    @DisplayName("order 를 생략하면 맨 뒤에 붙는다")
    void appendsWhenOrderOmitted() {
      seed("기존", "2025-FALL", 4);

      ProjectResult.Item created = projectAdminService.createProject(write("신규", "2025-FALL", null, null));

      assertThat(created.order()).isEqualTo(5);
    }

    @Test
    @DisplayName("order 를 주면 그 값을 쓴다")
    void usesExplicitOrder() {
      ProjectResult.Item created = projectAdminService.createProject(write("신규", "2025-FALL", null, 9));

      assertThat(created.order()).isEqualTo(9);
    }
  }

  @Nested
  @DisplayName("썸네일 fileId (이슈 #139)")
  class ThumbnailFileId {

    private Long saveFile(String key) {
      return TestStoredFiles.stored(fileAssetRepository, fileStorage,
          key, key + ".png", "https://cdn/" + key + ".png", 10L, "image/png", 1L).getId();
    }

    private FileStatus statusOf(Long fileId) {
      return fileAssetRepository.findById(fileId).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("등록하면 썸네일 파일을 연결한다 (이슈 #218)")
    void connectsThumbnailOnCreate() {
      Long fileId = saveFile("conn");

      projectAdminService.createProject(write("t", "2025-FALL", fileId, 1));

      // 연결해 두지 않으면 미연결 파일 정리 배치가 24시간 뒤 썸네일을 지운다.
      assertThat(statusOf(fileId)).isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("썸네일을 교체하면 새 파일은 연결하고 이전 파일은 연결을 끊는다 (이슈 #218)")
    void swapsConnectionOnReplace() {
      Long oldFile = saveFile("swap-old");
      Long newFile = saveFile("swap-new");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", oldFile, 1)).id();

      projectAdminService.updateProject(id, write("t", "2025-FALL", newFile, 1));

      assertThat(statusOf(newFile)).isEqualTo(FileStatus.CONNECTED);
      assertThat(statusOf(oldFile)).isEqualTo(FileStatus.PENDING);
    }

    @Test
    @DisplayName("같은 파일로 다시 저장해도 연결이 유지된다 (이슈 #218)")
    void keepsConnectionWhenSameFile() {
      Long fileId = saveFile("same");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", fileId, 1)).id();

      projectAdminService.updateProject(id, write("제목만", "2025-FALL", fileId, 1));

      assertThat(statusOf(fileId)).isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("fileId 를 비우면 이전 파일의 연결을 끊는다 (이슈 #218)")
    void disconnectsWhenFileCleared() {
      Long fileId = saveFile("clear");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", fileId, 1)).id();

      projectAdminService.updateProject(id, write("t", "2025-FALL", null, 1));

      assertThat(statusOf(fileId)).isEqualTo(FileStatus.PENDING);
    }

    @Test
    @DisplayName("삭제하면 썸네일 연결을 끊는다 (이슈 #218)")
    void disconnectsOnDelete() {
      Long fileId = saveFile("del");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", fileId, 1)).id();

      projectAdminService.deleteProject(id);

      assertThat(statusOf(fileId)).isEqualTo(FileStatus.PENDING);
    }

    private String url(String key) {
      return "http://localhost:8080/api/public/files/" + key;
    }

    @Test
    @DisplayName("목록·수정 응답에 fileId 가 포함된다")
    void responseIncludesFileId() {
      Long fileId = saveFile("k1");
      Long id = projectAdminService.createProject(write("썸네일", "2025-FALL", fileId, 1)).id();

      var list = projectAdminService.getProjects("2025-FALL", null, PageRequest.of(0, 20));
      assertThat(list.content().get(0).fileId()).isEqualTo(fileId);

      ProjectResult.Item updated = projectAdminService.updateProject(id, write("수정", "2025-FALL", fileId, 1));
      assertThat(updated.fileId()).isEqualTo(fileId);
    }

    @Test
    @DisplayName("같은 fileId 로 다시 저장하면 썸네일이 유지된다")
    void keepsThumbnailWhenSameFileId() {
      Long fileId = saveFile("keep");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", fileId, 1)).id();

      ProjectResult.Item updated = projectAdminService.updateProject(id, write("제목만", "2025-FALL", fileId, 1));

      assertThat(updated.fileId()).isEqualTo(fileId);
      assertThat(updated.thumbnailUrl()).isEqualTo(url("keep"));
    }

    @Test
    @DisplayName("fileId 를 생략하면 썸네일이 제거된다 (full replace)")
    void clearsThumbnailWhenFileIdOmitted() {
      Long fileId = saveFile("gone");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", fileId, 1)).id();

      ProjectResult.Item updated = projectAdminService.updateProject(id, write("t", "2025-FALL", null, 1));

      assertThat(updated.fileId()).isNull();
      assertThat(updated.thumbnailUrl()).isNull();
    }

    @Test
    @DisplayName("다른 fileId 로 저장하면 썸네일이 교체된다")
    void replacesThumbnailWithNewFileId() {
      Long oldFile = saveFile("old");
      Long newFile = saveFile("new");
      Long id = projectAdminService.createProject(write("t", "2025-FALL", oldFile, 1)).id();

      ProjectResult.Item updated = projectAdminService.updateProject(id, write("t", "2025-FALL", newFile, 1));

      assertThat(updated.fileId()).isEqualTo(newFile);
      assertThat(updated.thumbnailUrl()).isEqualTo(url("new"));
    }
  }

  @Nested
  @DisplayName("updateProject / deleteProject")
  class UpdateDelete {

    @Test
    @DisplayName("내용과 order 를 수정한다")
    void updates() {
      Long id = seed("원래", "2025-FALL", 1).getId();

      ProjectResult.Item updated = projectAdminService.updateProject(id, write("변경", "2026-SPRING", null, 3));

      assertThat(updated.title()).isEqualTo("변경");
      assertThat(updated.semester()).isEqualTo("2026-SPRING");
      assertThat(updated.order()).isEqualTo(3);
    }

    @Test
    @DisplayName("삭제한다")
    void deletes() {
      Long id = seed("삭제대상", "2025-FALL", 1).getId();

      projectAdminService.deleteProject(id);

      assertThat(projectRepository.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("없는 프로젝트면 예외")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> projectAdminService.deleteProject(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("FESTA 출전 (이슈 #220)")
  class FestaEntry {

    private ProjectRequest.Write withFesta(Boolean entry, Integer festaOrder) {
      return new ProjectRequest.Write("t", "팀", "2025-FALL", "설명",
          List.of(), null, null, null, false, 1, entry, festaOrder);
    }

    @Test
    @DisplayName("등록할 때 출전과 순서를 저장하고 응답에 싣는다")
    void createsWithFesta() {
      ProjectResult.Item item = projectAdminService.createProject(withFesta(true, 2));

      assertThat(item.festaEntry()).isTrue();
      assertThat(item.festaOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("festaEntry 를 안 보내면 기본은 미출전이다")
    void defaultsToNotEntered() {
      ProjectResult.Item item = projectAdminService.createProject(withFesta(null, null));

      assertThat(item.festaEntry()).isFalse();
      assertThat(item.festaOrder()).isNull();
    }

    @Test
    @DisplayName("수정할 때 festaEntry 를 안 보내면 기존 출전 설정을 건드리지 않는다")
    void keepsFestaWhenOmitted() {
      Long id = projectAdminService.createProject(withFesta(true, 3)).id();

      ProjectResult.Item updated = projectAdminService.updateProject(id, withFesta(null, null));

      assertThat(updated.festaEntry()).isTrue();
      assertThat(updated.festaOrder()).isEqualTo(3);
    }

    @Test
    @DisplayName("출전을 끄면 순서도 비운다")
    void clearsOrderWhenDisabled() {
      Long id = projectAdminService.createProject(withFesta(true, 3)).id();

      ProjectResult.Item updated = projectAdminService.updateProject(id, withFesta(false, 3));

      assertThat(updated.festaEntry()).isFalse();
      assertThat(updated.festaOrder()).isNull();
    }
  }
}
