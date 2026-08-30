package com.getit.domain.setting.photo.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.photo.dto.ActivityPhotoRequest;
import com.getit.domain.setting.photo.entity.ActivityPhoto;
import com.getit.domain.setting.photo.repository.ActivityPhotoRepository;
import com.getit.domain.user.entity.Role;

/** /api/admin/setting/activity-photos (이슈 #146) */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ActivityPhotoControllerTest {

  private static final String PATH = "/api/admin/setting/activity-photos";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private ActivityPhotoRepository activityPhotoRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private Long publicFileId(String key) {
    return fileAssetRepository.save(FileAsset.upload(
        "public/" + key, key + ".png", "https://cdn/" + key, 10L, "image/png", 1L)).getId();
  }

  private String requestJson(Long fileId, Integer order) throws Exception {
    return objectMapper.writeValueAsString(new ActivityPhotoRequest(fileId, true, order));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);
      mockMvc.perform(get(PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("CRUD")
  class Crud {

    @Test
    @DisplayName("등록하면 201 과 함께 발급된 순서를 준다")
    void createsPhoto() throws Exception {
      mockMvc.perform(post(PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(publicFileId("p1"), null)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data.order").value(1))
          .andExpect(jsonPath("$.data.imageUrl").isNotEmpty());
    }

    @Test
    @DisplayName("fileId 가 없으면 400 이다")
    void rejectsMissingFileId() throws Exception {
      mockMvc.perform(post(PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content("{\"isVisible\": true}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("숨긴 사진도 목록에 나온다")
    void listsHiddenPhotosToo() throws Exception {
      activityPhotoRepository.save(ActivityPhoto.create(publicFileId("hidden"), 1, false));

      mockMvc.perform(get(PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].isVisible").value(false));
    }

    @Test
    @DisplayName("수정하면 바뀐 값을 준다")
    void updatesPhoto() throws Exception {
      ActivityPhoto photo =
          activityPhotoRepository.save(ActivityPhoto.create(publicFileId("p1"), 1, true));

      mockMvc.perform(put(PATH + "/" + photo.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(
                  new ActivityPhotoRequest(photo.getFileId(), false, null))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.isVisible").value(false));
    }

    @Test
    @DisplayName("삭제하면 204 이고 없는 id 는 404 다")
    void deletesPhoto() throws Exception {
      ActivityPhoto photo =
          activityPhotoRepository.save(ActivityPhoto.create(publicFileId("p1"), 1, true));

      mockMvc.perform(delete(PATH + "/" + photo.getId()).header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      mockMvc.perform(delete(PATH + "/999999").header("Authorization", adminToken()))
          .andExpect(status().isNotFound());
    }
  }
}
