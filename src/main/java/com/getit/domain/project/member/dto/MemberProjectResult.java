package com.getit.domain.project.member.dto;

import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
import java.util.List;

/**
 * 부원이 낸 프로젝트의 응답. (이슈 #148)
 *
 * <p>{@code status} 를 함께 준다. 내고 나서 바로 공개되는 게 아니라 승인을 기다린다는 것을
 * 화면이 알려줘야 한다.
 */
public record MemberProjectResult(
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
    ProjectStatus status,
    String statusLabel
) {

  public static MemberProjectResult of(Project project, String thumbnailUrl) {
    return new MemberProjectResult(
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
        project.getStatus(),
        project.getStatus().getLabel());
  }
}
