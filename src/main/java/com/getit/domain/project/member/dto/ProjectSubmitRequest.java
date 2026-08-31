package com.getit.domain.project.member.dto;

import com.getit.domain.project.dto.ProjectCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 부원이 내는 프로젝트. (이슈 #148)
 *
 * <p>팀 이름은 받지 않는다. 자기 조 명의로 내는 것이라 서버가 조 이름을 붙인다.
 * 추천 배치({@code isFeatured})와 노출 순서도 받지 않는다 — 공개 홈의 큐레이션은 어드민 몫이다.
 */
public record ProjectSubmitRequest(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Pattern(regexp = "\\d{4}-(SPRING|SUMMER|FALL|WINTER)") String semester,
    String description,
    @Size(max = 10) List<@Size(max = 40) @Pattern(regexp = "[^,]+") String> techStacks,
    @Size(max = 512) String codeUrl,
    @Size(max = 512) String demoUrl,
    @Positive Long fileId
) {

  public ProjectCommand toCommand(String teamName) {
    return new ProjectCommand(
        title, teamName, semester, description,
        techStacks == null ? List.of() : techStacks,
        codeUrl, demoUrl, false, fileId);
  }
}
