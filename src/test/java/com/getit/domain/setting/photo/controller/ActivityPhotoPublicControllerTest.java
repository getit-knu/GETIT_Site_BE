package com.getit.domain.setting.photo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;

/** /api/public/activity-photos (이슈 #146) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActivityPhotoPublicControllerTest {

  private static final String PATH = "/api/public/activity-photos";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ActivityPhotoRepository activityPhotoRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private void photo(String key, int order, boolean visible) {
    FileAsset file = fileAssetRepository.save(FileAsset.upload(
        "public/" + key, key + ".png", "https://cdn/" + key, 10L, "image/png", 1L));
    activityPhotoRepository.save(ActivityPhoto.create(file.getId(), order, visible));
  }

  @Test
  @DisplayName("인증 없이 조회할 수 있고 숨긴 사진은 빠진다")
  void anonymousCanRead() throws Exception {
    photo("a", 1, true);
    photo("b", 2, false);
    photo("c", 3, true);

    // 홈은 로그인 전에도 보이는 화면이다. 여기서 401 이 나면 마퀴가 통째로 비어 보인다.
    mockMvc.perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].order").value(1))
        .andExpect(jsonPath("$.data[1].order").value(3))
        .andExpect(jsonPath("$.data[0].imageUrl").isNotEmpty());
  }

  @Test
  @DisplayName("사진이 없으면 빈 배열이다")
  void emptyArrayWhenNoPhotos() throws Exception {
    mockMvc.perform(get(PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0));
  }
}
