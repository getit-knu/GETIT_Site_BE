package com.getit.domain.project.member.service;

import com.getit.domain.file.service.FileConnectionService;
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
    String teamName = memberGroupService.findMyGroup(userId)
        .map(GroupWithMembersResult::name)
        .orElseThrow(() -> new BusinessException(ProjectErrorCode.NOT_ASSIGNED_TO_GROUP));

    int order = projectRepository.findMaxOrder() + 1;
    Project saved = projectRepository.save(Project.submit(request.toCommand(teamName), order));

    // 연결해 두지 않으면 미연결 파일 정리 배치(OrphanFileCleaner)가 썸네일을 지운다.
    if (saved.getFileId() != null) {
      fileConnectionService.connectAll(List.of(saved.getFileId()));
    }
    return MemberProjectResult.of(saved, thumbnailUrl(saved.getFileId()));
  }

  private String thumbnailUrl(Long fileId) {
    return fileId == null ? null : fileQueryService.findById(fileId).url();
  }
}
