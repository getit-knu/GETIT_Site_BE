package com.getit.domain.setting.category.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.category.dto.CategoryRequest.SubCategoryCreate;
import com.getit.domain.setting.category.dto.CategoryRequest.SubCategoryUpdate;
import com.getit.domain.setting.category.dto.CategoryRequest.TrackCreate;
import com.getit.domain.setting.category.dto.CategoryRequest.TrackUpdate;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.user.entity.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 10.3~10.9 /api/admin/setting/tracks · /subcategories */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CategoryControllerTest {

  private static final String TRACKS_PATH = "/api/admin/setting/tracks";
  private static final String SUBCATEGORIES_PATH = "/api/admin/setting/subcategories";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(2L, "member@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(TRACKS_PATH))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      mockMvc.perform(get(TRACKS_PATH).header("Authorization", memberToken()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + TRACKS_PATH)
  class GetCategoryTree {

    @Test
    @DisplayName("트랙-소분류 트리를 반환한다")
    void returnsTree() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      subCategoryRepository.save(SubCategory.create("웹기초", 1, track.getId()));

      mockMvc.perform(get(TRACKS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          .andExpect(jsonPath("$.data[0].name").value("SW"))
          .andExpect(jsonPath("$.data[0].subCategories[0].name").value("웹기초"))
          .andExpect(jsonPath("$.data[0].subCategories[0].lectureCount").value(0));
    }
  }

  @Nested
  @DisplayName("POST " + TRACKS_PATH)
  class CreateTrack {

    @Test
    @DisplayName("생성에 성공하면 201과 lectureCount 0을 반환한다")
    void createsTrack() throws Exception {
      String body = objectMapper.writeValueAsString(new TrackCreate("세미나"));

      mockMvc.perform(post(TRACKS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.name").value("세미나"))
          .andExpect(jsonPath("$.data.order").value(1))
          .andExpect(jsonPath("$.data.lectureCount").value(0));
    }

    @Test
    @DisplayName("name이 비어 있으면 400 이다")
    void returns400WhenNameBlank() throws Exception {
      String body = objectMapper.writeValueAsString(new TrackCreate(""));

      mockMvc.perform(post(TRACKS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("PUT " + TRACKS_PATH + "/{id}")
  class UpdateTrack {

    @Test
    @DisplayName("수정에 성공하면 200과 변경된 필드를 반환한다")
    void updatesTrack() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      String body = objectMapper.writeValueAsString(new TrackUpdate("SW 개편", 2));

      mockMvc.perform(put(TRACKS_PATH + "/" + track.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("SW 개편"))
          .andExpect(jsonPath("$.data.order").value(2));
    }

    @Test
    @DisplayName("존재하지 않는 트랙이면 404 다")
    void returns404WhenNotFound() throws Exception {
      String body = objectMapper.writeValueAsString(new TrackUpdate("SW 개편", 1));

      mockMvc.perform(put(TRACKS_PATH + "/999999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("TRACK_NOT_FOUND"));
    }

    @Test
    @DisplayName("order가 1보다 작으면 400 이다")
    void returns400WhenOrderBelowMinimum() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      String body = objectMapper.writeValueAsString(new TrackUpdate("SW 개편", 0));

      mockMvc.perform(put(TRACKS_PATH + "/" + track.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("DELETE " + TRACKS_PATH + "/{id}")
  class DeleteTrack {

    @Test
    @DisplayName("연결된 강의가 없으면 204 다")
    void deletesTrack() throws Exception {
      Track track = trackRepository.save(Track.create("세미나", 1));

      mockMvc.perform(delete(TRACKS_PATH + "/" + track.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 트랙이면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(delete(TRACKS_PATH + "/999999")
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("TRACK_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("POST " + SUBCATEGORIES_PATH)
  class CreateSubCategory {

    @Test
    @DisplayName("존재하지 않는 트랙이면 404 다")
    void returns404WhenTrackNotFound() throws Exception {
      String body = objectMapper.writeValueAsString(new SubCategoryCreate(999999L, "웹기초"));

      mockMvc.perform(post(SUBCATEGORIES_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("TRACK_NOT_FOUND"));
    }

    @Test
    @DisplayName("생성에 성공하면 201을 반환한다")
    void createsSubCategory() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      String body = objectMapper.writeValueAsString(new SubCategoryCreate(track.getId(), "웹기초"));

      mockMvc.perform(post(SUBCATEGORIES_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.name").value("웹기초"))
          .andExpect(jsonPath("$.data.trackId").value(track.getId()));
    }
  }

  @Nested
  @DisplayName("PUT " + SUBCATEGORIES_PATH + "/{id}")
  class UpdateSubCategory {

    @Test
    @DisplayName("수정에 성공하면 200과 변경된 필드를 반환한다")
    void updatesSubCategory() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      SubCategory subCategory = subCategoryRepository.save(SubCategory.create("웹기초", 1, track.getId()));
      String body = objectMapper.writeValueAsString(new SubCategoryUpdate("웹심화", 2));

      mockMvc.perform(put(SUBCATEGORIES_PATH + "/" + subCategory.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("웹심화"))
          .andExpect(jsonPath("$.data.order").value(2))
          .andExpect(jsonPath("$.data.trackId").value(track.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 소분류면 404 다")
    void returns404WhenNotFound() throws Exception {
      String body = objectMapper.writeValueAsString(new SubCategoryUpdate("웹심화", 1));

      mockMvc.perform(put(SUBCATEGORIES_PATH + "/999999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SUBCATEGORY_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + SUBCATEGORIES_PATH + "/{id}")
  class DeleteSubCategory {

    @Test
    @DisplayName("연결된 강의가 없으면 204 다")
    void deletesSubCategory() throws Exception {
      Track track = trackRepository.save(Track.create("SW", 1));
      SubCategory subCategory = subCategoryRepository.save(SubCategory.create("웹기초", 1, track.getId()));

      mockMvc.perform(delete(SUBCATEGORIES_PATH + "/" + subCategory.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("존재하지 않는 소분류면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(delete(SUBCATEGORIES_PATH + "/999999")
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SUBCATEGORY_NOT_FOUND"));
    }
  }
}
