package com.getit.domain.setting.photo.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.setting.photo.dto.ActivityPhotoRequest;
import com.getit.domain.setting.photo.dto.ActivityPhotoResult;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.exception.ActivityPhotoErrorCode;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;
import com.getit.global.exception.BusinessException;

/**
 * 활동 사진 관리. (이슈 #146)
 *
 * <p>{@code order} 규칙은 FAQ 와 같다. 클라이언트가 값을 보내고, 별도 순서 변경
 * 엔드포인트는 두지 않는다. 1..N 연속을 서버가 유지한다.
 *
 * <p>사진 파일은 file 도메인이 보관한다. 등록·교체·삭제 때 연결 상태를 함께 옮긴다.
 * 연결하지 않으면 미연결 파일 정리 배치가 24시간 뒤 지워버린다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ActivityPhotoAdminService {

  private final ActivityPhotoRepository activityPhotoRepository;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  @Transactional(readOnly = true)
  public List<ActivityPhotoResult> getPhotos() {
    List<ActivityPhoto> photos = activityPhotoRepository.findAllByOrderByOrderAsc();
    Map<Long, String> urls = imageUrls(photos);
    return photos.stream()
        .map(photo -> ActivityPhotoResult.from(photo, urls.get(photo.getFileId())))
        .toList();
  }

  /** order 를 생략하면 맨 뒤에 붙이고, 값이 있으면 [1, 기존 개수+1] 로 맞춘 뒤 뒤를 민다. */
  public ActivityPhotoResult create(ActivityPhotoRequest request) {
    validateAndConnect(request.fileId());

    List<ActivityPhoto> siblings = activityPhotoRepository.findAllByOrderByOrderAsc();
    int newOrder = request.order() == null
        ? siblings.size() + 1
        : clamp(request.order(), 1, siblings.size() + 1);

    siblings.stream()
        .filter(photo -> photo.getOrder() >= newOrder)
        .forEach(photo -> photo.updateOrder(photo.getOrder() + 1));

    ActivityPhoto saved = activityPhotoRepository.save(
        ActivityPhoto.create(request.fileId(), newOrder, request.isVisible()));

    return ActivityPhotoResult.from(saved, imageUrl(saved.getFileId()));
  }

  public ActivityPhotoResult update(Long id, ActivityPhotoRequest request) {
    ActivityPhoto target = findPhoto(id);

    Long oldFileId = target.getFileId();
    if (!oldFileId.equals(request.fileId())) {
      validateAndConnect(request.fileId());
      fileConnectionService.disconnectAll(List.of(oldFileId));
    }
    target.update(request.fileId(), request.isVisible());

    if (request.order() != null) {
      moveOrder(activityPhotoRepository.findAllByOrderByOrderAsc(), target, request.order());
    }
    return ActivityPhotoResult.from(target, imageUrl(target.getFileId()));
  }

  public void delete(Long id) {
    ActivityPhoto target = findPhoto(id);
    int deletedOrder = target.getOrder();

    fileConnectionService.disconnectAll(List.of(target.getFileId()));
    activityPhotoRepository.delete(target);

    // 지운 자리 뒤를 당겨 1..N 연속을 유지한다.
    activityPhotoRepository.findAllByOrderByOrderAsc().stream()
        .filter(photo -> photo.getOrder() > deletedOrder)
        .forEach(photo -> photo.updateOrder(photo.getOrder() - 1));
  }

  private void moveOrder(List<ActivityPhoto> siblings, ActivityPhoto target, int requested) {
    int to = clamp(requested, 1, siblings.size());
    int from = target.getOrder();
    if (from == to) {
      return;
    }
    siblings.stream()
        .filter(photo -> !photo.getId().equals(target.getId()))
        .forEach(photo -> {
          int order = photo.getOrder();
          if (from < to && order > from && order <= to) {
            photo.updateOrder(order - 1);
          } else if (from > to && order >= to && order < from) {
            photo.updateOrder(order + 1);
          }
        });
    target.updateOrder(to);
  }

  /** 파일이 실제로 있는지 확인하고 연결한다. 연결하지 않으면 정리 배치가 지운다. */
  private void validateAndConnect(Long fileId) {
    fileQueryService.findById(fileId);
    fileConnectionService.connectAll(List.of(fileId));
  }

  private ActivityPhoto findPhoto(Long id) {
    return activityPhotoRepository.findById(id)
        .orElseThrow(() -> new BusinessException(ActivityPhotoErrorCode.ACTIVITY_PHOTO_NOT_FOUND));
  }

  private String imageUrl(Long fileId) {
    return fileQueryService.findById(fileId).url();
  }

  /** 목록은 파일을 한 번에 조회한다. 사진마다 조회하면 N+1 이 된다. */
  private Map<Long, String> imageUrls(Collection<ActivityPhoto> photos) {
    List<Long> fileIds = photos.stream().map(ActivityPhoto::getFileId).distinct().toList();
    if (fileIds.isEmpty()) {
      return Map.of();
    }
    return fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
