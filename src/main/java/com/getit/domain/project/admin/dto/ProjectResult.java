package com.getit.domain.project.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.getit.domain.project.dto.ProjectImages;
import com.getit.domain.project.entity.ProgressStatus;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
import java.util.List;

public class ProjectResult {

  public record Item(
      Long id,
      String title,
      String teamName,
      String semester,
      String description,
      List<String> techStacks,
      String codeUrl,
      String demoUrl,
      Long fileId,
      String thumbnailUrl,
      @JsonProperty("isFeatured") boolean isFeatured,
      int order,
      ProjectStatus status,
      String statusLabel,
      String rejectReason,
      boolean festaEntry,
      Integer festaOrder,
      String oneLiner,
      ProgressStatus progressStatus,
      String progressStatusLabel,
      String playStoreUrl,
      String appStoreUrl,
      List<Long> galleryFileIds,
      List<String> galleryImageUrls,
      List<Long> descriptionImageFileIds
  ) {

    public static Item of(Project project, String thumbnailUrl, ProjectImages images) {
      return new Item(
          project.getId(),
          project.getTitle(),
          project.getTeamName(),
          project.getSemester(),
          project.getDescription(),
          project.getTechStacks(),
          project.getCodeUrl(),
          project.getDemoUrl(),
          project.getFileId(),
          thumbnailUrl,
          project.isFeatured(),
          project.getOrder(),
          project.getStatus(),
          project.getStatus().getLabel(),
          project.getRejectReason(),
          project.isFestaEntry(),
          project.getFestaOrder(),
          project.getOneLiner(),
          project.getProgressStatus(),
          project.getProgressStatus() == null ? null : project.getProgressStatus().getLabel(),
          project.getPlayStoreUrl(),
          project.getAppStoreUrl(),
          images.galleryFileIds(),
          images.galleryImageUrls(),
          images.descriptionImageFileIds());
    }
  }
}
