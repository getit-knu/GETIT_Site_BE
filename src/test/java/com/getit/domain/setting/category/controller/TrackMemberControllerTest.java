package com.getit.domain.setting.category.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.user.entity.Role;

/**
 * 부원용 트랙 목록. (이슈 #150)
 *
 * <p>강의 목록의 {@code tabs} 는 소분류 단위라 소분류 없는 트랙이 통째로 빠진다.
 * 이 엔드포인트는 그런 트랙까지 보여주는 것이 존재 이유다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TrackMemberControllerTest {

  private static final String PATH = "/api/member/tracks";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);
  }

  @BeforeEach
  void setUp() {
    subCategoryRepository.deleteAll();
    trackRepository.deleteAll();
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      // 인증 진입점 설정이 바뀌면 여기서 잡힌다.
      mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("부원이 아니면 403 이다")
    void rejectsGuest() throws Exception {
      String guestToken = "Bearer " + jwtProvider.createAccessToken(1L, "guest@getit.com", Role.GUEST);

      mockMvc.perform(get(PATH).header("Authorization", guestToken))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("조회")
  class Read {

    @Test
    @DisplayName("소분류가 없는 트랙도 목록에 나온다")
    void includesTrackWithoutSubCategories() throws Exception {
      Track withSub = trackRepository.save(Track.create("SW 개발", 1));
      subCategoryRepository.save(SubCategory.create("WEB 기초", 1, withSub.getId()));
      trackRepository.save(Track.create("창업 빌드업", 2));

      mockMvc.perform(get(PATH).header("Authorization", memberToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.success").value(true))
          // 소분류가 없다고 트랙이 통째로 빠지면 화면에서 존재 자체를 알 수 없다.
          .andExpect(jsonPath("$.data.length()").value(2))
          .andExpect(jsonPath("$.data[1].name").value("창업 빌드업"))
          .andExpect(jsonPath("$.data[1].subCategories.length()").value(0));
    }

    @Test
    @DisplayName("트랙에 딸린 소분류를 함께 준다")
    void includesSubCategories() throws Exception {
      Track track = trackRepository.save(Track.create("SW 개발", 1));
      subCategoryRepository.save(SubCategory.create("WEB 기초", 1, track.getId()));

      mockMvc.perform(get(PATH).header("Authorization", memberToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].subCategories[0].name").value("WEB 기초"));
    }
  }
}
