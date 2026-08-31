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
 * <p>{@code description} 은 컬럼이 TEXT(65535 바이트) 다. 한글은 UTF-8 에서 글자당
 * 3 바이트라 최악의 경우를 잡아 20000 자로 끊는다. 상한이 없으면 검증을 통과한 뒤
 * 저장 단계에서 500 이 난다 (PR #165 리뷰 지적).
 *
 * <p>팀 이름은 받지 않는다. 자기 조 명의로 내는 것이라 서버가 조 이름을 붙인다.
 * 추천 배치({@code isFeatured})와 노출 순서도 받지 않는다 — 공개 홈의 큐레이션은 어드민 몫이다.
 */
public record ProjectSubmitRequest(
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Pattern(regexp = "\\d{4}-(SPRING|SUMMER|FALL|WINTER)") String semester,
    @Size(max = 20000) String description,
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
