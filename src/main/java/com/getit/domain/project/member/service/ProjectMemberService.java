package com.getit.domain.project.member.service;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.entity.Project;
import com.getit.domain.project.exception.ProjectErrorCode;
import com.getit.domain.project.member.dto.MemberProjectResult;
import com.getit.domain.project.member.dto.ProjectSubmitRequest;
import com.getit.domain.project.repository.ProjectRepository;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.service.MemberGroupService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 부원이 자기 조 명의로 내는 프로젝트. (이슈 #148) */
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectMemberService {

  private final ProjectRepository projectRepository;
  private final MemberGroupService memberGroupService;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  /**
   * 등록하면 승인 대기 상태로 들어간다. 어드민이 승인해야 공개 쇼케이스(2.4)에 나온다.
   *
   * <p>팀 이름은 요청에서 받지 않고 조 이름을 붙인다. 받으면 남의 조 이름으로 낼 수 있다.
   */
  public MemberProjectResult submitProject(Long userId, ProjectSubmitRequest request) {
    GroupWithMembersResult group = memberGroupService.findMyGroup(userId)
        .orElseThrow(() -> new BusinessException(ProjectErrorCode.NOT_ASSIGNED_TO_GROUP));

    // 동시 제출 둘이 같은 max + 1 을 읽어 order 가 겹칠 수 있다. 겹쳐도 목록 정렬은
    // order 다음에 id 로 갈리므로 순서가 흔들리지 않는다. 어드민이 order 를 직접 넣는
    // 경로에도 유일성 제약이 없어, 여기만 직렬화해도 얻는 게 없다 (PR #165 리뷰).
    int order = projectRepository.findMaxOrder() + 1;
    Project saved = projectRepository.save(
        Project.submit(request.toCommand(group.name()), order, group.id()));

    // 연결해 두지 않으면 미연결 파일 정리 배치(OrphanFileCleaner)가 썸네일을 지운다.
    if (saved.getFileId() != null) {
      fileConnectionService.connectAll(List.of(saved.getFileId()));
    }
    return MemberProjectResult.of(saved, thumbnailUrl(saved.getFileId()));
  }

  /**
   * 우리 조가 낸 프로젝트 목록. (이슈 #190)
   *
   * <p>등록만 있고 조회가 없어서, 반려 사유를 남겨도 부원이 볼 방법이 없었다.
   *
   * <p>조에 배정되지 않았으면 빈 목록이다. 등록은 조가 있어야 하므로 낸 것도 없다 —
   * 여기서 오류를 내면 화면이 "조가 없다" 와 "낸 것이 없다" 를 따로 다뤄야 한다.
   */
  @Transactional(readOnly = true)
  public List<MemberProjectResult> getMyProjects(Long userId) {
    return memberGroupService.findMyGroup(userId)
        .map(group -> toResults(projectRepository.findByGroupIdOrderByIdDesc(group.id())))
        .orElseGet(List::of);
  }

  /** 썸네일은 한 번에 읽는다. 줄마다 조회하면 프로젝트 수만큼 쿼리가 나간다 (PR #197 리뷰). */
  private List<MemberProjectResult> toResults(List<Project> projects) {
    List<Long> fileIds = projects.stream()
        .map(Project::getFileId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    Map<Long, String> urlByFileId = fileIds.isEmpty()
        ? Map.of()
        : fileQueryService.findAllByIds(fileIds).stream()
            .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));

    // Map.of() 같은 불변 맵은 null 키로 조회하면 NPE 를 던진다. 썸네일이 없는 프로젝트가
    // 섞여 있으므로 먼저 걸러낸다 (이슈 #142 에서 단과대 이름에 같은 문제가 있었다).
    return projects.stream()
        .map(project -> MemberProjectResult.of(project,
            project.getFileId() == null ? null : urlByFileId.get(project.getFileId())))
        .toList();
  }

  private String thumbnailUrl(Long fileId) {
    return fileId == null ? null : fileQueryService.findById(fileId).url();
  }
}
