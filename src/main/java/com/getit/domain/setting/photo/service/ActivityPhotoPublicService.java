package com.getit.domain.setting.photo.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.setting.photo.dto.ActivityPhotoPublicResult;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;

/** 공개 홈 마퀴용 조회. 노출 대상만 순서대로 준다. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ActivityPhotoPublicService {

  private final ActivityPhotoRepository activityPhotoRepository;
  private final FileQueryService fileQueryService;

  public List<ActivityPhotoPublicResult> getPhotos() {
    List<ActivityPhoto> photos = activityPhotoRepository.findAllByIsVisibleTrueOrderByOrderAsc();
    if (photos.isEmpty()) {
      return List.of();
    }

    // 사진마다 파일을 조회하면 N+1 이 된다. 한 번에 받아 매핑한다.
    Map<Long, String> urls = fileQueryService
        .findAllByIds(photos.stream().map(ActivityPhoto::getFileId).distinct().toList())
        .stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));

    return photos.stream()
        .map(photo -> ActivityPhotoPublicResult.from(photo, urls.get(photo.getFileId())))
        .toList();
  }
}
