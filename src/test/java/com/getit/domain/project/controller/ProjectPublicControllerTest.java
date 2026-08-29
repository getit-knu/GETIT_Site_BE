package com.getit.domain.project.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.project.dto.ProjectCommand;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.repository.ProjectRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.4 GET /api/public/projects */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectPublicControllerTest {

  private static final String PROJECTS_PATH = "/api/public/projects";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ProjectRepository projectRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private void seed(String title, String semester, int order, Long fileId) {
    projectRepository.save(Project.create(new ProjectCommand(
        title, "팀", semester, "설명", List.of("React"), null, null, false, fileId), order));
  }

  @Test
  @DisplayName("인증 없이 학기 필터 + order 순 + 페이지 메타로 반환한다")
  void returnsShowcaseWithoutAuthentication() throws Exception {
    seed("B", "2025-FALL", 2, null);
    seed("A", "2025-FALL", 1, null);
    seed("C", "2024-SPRING", 1, null);

    mockMvc.perform(get(PROJECTS_PATH).param("semester", "2025-FALL").param("size", "9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.semesters").value(contains("2025-FALL", "2024-SPRING")))
        .andExpect(jsonPath("$.data.content.length()").value(2))
        .andExpect(jsonPath("$.data.content[0].title").value("A"))
        .andExpect(jsonPath("$.data.content[0].semester").value("2025-FALL"))
        .andExpect(jsonPath("$.data.content[0].semesterLabel").value("2025 Fall"))
        .andExpect(jsonPath("$.data.content[0].techStacks[0]").value("React"))
        .andExpect(jsonPath("$.data.size").value(9))
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.first").value(true));
  }

  @Test
  @DisplayName("썸네일은 살아있는 파일이면 url, soft-delete 됐거나 없으면 null 이다")
  void resolvesThumbnailUrl() throws Exception {
    FileAsset live = fileAssetRepository.save(FileAsset.upload(
        "k1", "live.png", "https://cdn.getit.com/live.png", 10L, "image/png", 1L));
    FileAsset removed = fileAssetRepository.save(FileAsset.upload(
        "k2", "gone.png", "https://cdn.getit.com/gone.png", 10L, "image/png", 1L));
    removed.delete();
    fileAssetRepository.flush();

    seed("살아있음", "2025-FALL", 1, live.getId());
    seed("삭제됨", "2025-FALL", 2, removed.getId());
    seed("없음", "2025-FALL", 3, null);

    mockMvc.perform(get(PROJECTS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].thumbnailUrl").value("http://localhost:8080/api/public/files/k1"))
        .andExpect(jsonPath("$.data.content[1].thumbnailUrl").isEmpty())
        .andExpect(jsonPath("$.data.content[2].thumbnailUrl").isEmpty());
  }
}
