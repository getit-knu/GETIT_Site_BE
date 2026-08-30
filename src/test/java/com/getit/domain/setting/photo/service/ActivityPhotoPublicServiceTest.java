package com.getit.domain.setting.photo.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.photo.dto.ActivityPhotoPublicResult;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;

@SpringBootTest
@Transactional
class ActivityPhotoPublicServiceTest {

  @Autowired
  private ActivityPhotoPublicService activityPhotoPublicService;

  @Autowired
  private ActivityPhotoRepository activityPhotoRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private ActivityPhoto photo(String key, int order, boolean visible) {
    FileAsset file = fileAssetRepository.save(FileAsset.upload(
        "public/" + key, key + ".png", "https://cdn/" + key, 10L, "image/png", 1L));
    return activityPhotoRepository.save(ActivityPhoto.create(file.getId(), order, visible));
  }

  @Test
  @DisplayName("숨긴 사진은 내려가지 않고 순서대로 정렬된다")
  void returnsOnlyVisibleInOrder() {
    photo("c", 3, true);
    photo("a", 1, true);
    photo("b", 2, false);

    var result = activityPhotoPublicService.getPhotos();

    assertThat(result).extracting(ActivityPhotoPublicResult::order).containsExactly(1, 3);
    assertThat(result).allSatisfy(p -> assertThat(p.imageUrl()).isNotBlank());
  }

  @Test
  @DisplayName("사진이 없으면 빈 목록이다")
  void emptyWhenNoPhotos() {
    // 파일을 한 번에 조회하는 경로라 빈 입력에서 터지지 않아야 한다.
    assertThat(activityPhotoPublicService.getPhotos()).isEmpty();
  }
}
