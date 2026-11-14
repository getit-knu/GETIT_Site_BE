package com.getit.domain.project.service;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.dto.ProjectImages;
import com.getit.domain.project.entity.ProjectImage;
import com.getit.domain.project.entity.ProjectImageKind;
import com.getit.domain.project.exception.ProjectErrorCode;
import com.getit.domain.project.repository.ProjectImageRepository;
import com.getit.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로젝트 이미지(갤러리 · 설명 속 이미지)의 저장과 조회. (이슈 #221)
 *
 * <p>연결 대상은 FE 가 보내는 id 목록으로만 판단한다. 서버는 설명 본문을 파싱하지 않는다.
 * 연결해 두지 않으면 미연결 파일 정리 배치가 24시간 뒤 이미지를 지운다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectImageService {

  private final ProjectImageRepository projectImageRepository;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  /**
   * 두 목록을 요청 내용으로 통째로 바꾼다. 갤러리는 목록 순서가 곧 노출 순서다.
   *
   * <p>빠진 파일은 연결을 끊고, 새 파일은 연결한다. 이미 있던 파일은 순서만 맞춘다.
   * {@code null} 은 빈 목록과 같다 — 프로젝트 수정은 통째로 바꾸는 요청이다.
   */
  @Transactional
  public void replace(Long projectId, List<Long> galleryFileIds, List<Long> descriptionFileIds) {
    Map<Long, Slot> requested = toRequested(galleryFileIds, descriptionFileIds);
    List<ProjectImage> existing = projectImageRepository.findByProjectIdOrderByKindAscSortOrderAsc(projectId);

    List<ProjectImage> toRemove = new ArrayList<>();
    Set<Long> kept = new HashSet<>();
    for (ProjectImage image : existing) {
      Slot slot = requested.get(image.getFileId());
      if (slot == null || slot.kind() != image.getKind()) {
        toRemove.add(image);
        continue;
      }
      image.moveTo(slot.sortOrder());
      kept.add(image.getFileId());
    }

    fileConnectionService.disconnectAll(toRemove.stream().map(ProjectImage::getFileId).toList());
    projectImageRepository.deleteAll(toRemove);
    // 종류만 바뀐 파일은 같은 file_id 로 다시 넣는다. 지운 행이 먼저 나가야 유일 제약에 걸리지 않는다.
    projectImageRepository.flush();

    List<Long> toAddIds = requested.keySet().stream().filter(fileId -> !kept.contains(fileId)).toList();
    fileConnectionService.connectAll(toAddIds);
    projectImageRepository.saveAll(toAddIds.stream()
        .map(fileId -> ProjectImage.of(projectId, fileId, requested.get(fileId).kind(), requested.get(fileId).sortOrder()))
        .toList());
  }

  /** 프로젝트를 지울 때 이미지 연결을 끊고 행을 지운다. */
  @Transactional
  public void removeAll(Long projectId) {
    replace(projectId, null, null);
  }

  public ProjectImages findOne(Long projectId) {
    return findAll(List.of(projectId)).getOrDefault(projectId, ProjectImages.EMPTY);
  }

  /** 이미지 행과 갤러리 URL 을 각각 한 번에 읽는다. 프로젝트 수와 무관하게 쿼리 2회다. */
  public Map<Long, ProjectImages> findAll(Collection<Long> projectIds) {
    if (projectIds.isEmpty()) {
      return Map.of();
    }
    List<ProjectImage> rows =
        projectImageRepository.findByProjectIdInOrderByProjectIdAscKindAscSortOrderAsc(projectIds);
    List<Long> galleryFileIds = rows.stream()
        .filter(image -> image.getKind() == ProjectImageKind.GALLERY)
        .map(ProjectImage::getFileId)
        .distinct()
        .toList();
    Map<Long, String> urlByFileId = galleryFileIds.isEmpty()
        ? Map.of()
        : fileQueryService.findAllByIds(galleryFileIds).stream()
            .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));

    Map<Long, List<ProjectImage>> byProject = rows.stream()
        .collect(Collectors.groupingBy(ProjectImage::getProjectId));
    Map<Long, ProjectImages> result = new HashMap<>();
    byProject.forEach((projectId, images) -> {
      List<Long> gallery = idsOf(images, ProjectImageKind.GALLERY);
      result.put(projectId, new ProjectImages(
          gallery,
          gallery.stream().map(urlByFileId::get).filter(java.util.Objects::nonNull).toList(),
          idsOf(images, ProjectImageKind.DESCRIPTION)));
    });
    return result;
  }

  private static List<Long> idsOf(List<ProjectImage> images, ProjectImageKind kind) {
    return images.stream()
        .filter(image -> image.getKind() == kind)
        .map(ProjectImage::getFileId)
        .toList();
  }

  /** 같은 파일을 두 번 넣는 요청은 거부한다. 한 파일은 한 자리에만 연결된다. */
  private static Map<Long, Slot> toRequested(List<Long> galleryFileIds, List<Long> descriptionFileIds) {
    Map<Long, Slot> requested = new LinkedHashMap<>();
    addAll(requested, galleryFileIds, ProjectImageKind.GALLERY);
    addAll(requested, descriptionFileIds, ProjectImageKind.DESCRIPTION);
    return requested;
  }

  private static void addAll(Map<Long, Slot> requested, List<Long> fileIds, ProjectImageKind kind) {
    if (fileIds == null) {
      return;
    }
    for (int index = 0; index < fileIds.size(); index++) {
      if (requested.putIfAbsent(fileIds.get(index), new Slot(kind, index)) != null) {
        throw new BusinessException(ProjectErrorCode.DUPLICATE_PROJECT_IMAGE);
      }
    }
  }

  private record Slot(ProjectImageKind kind, int sortOrder) { }
}
