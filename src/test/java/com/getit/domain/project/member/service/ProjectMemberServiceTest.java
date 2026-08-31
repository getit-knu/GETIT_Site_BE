package com.getit.domain.project.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.TestStoredFiles;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.project.admin.dto.ProjectRequest;
import com.getit.domain.project.admin.dto.ProjectResult;
import com.getit.domain.project.admin.service.ProjectAdminService;
import com.getit.domain.project.dto.ProjectShowcaseResult;
import com.getit.domain.project.entity.ProjectStatus;
import com.getit.domain.project.exception.ProjectErrorCode;
import com.getit.domain.project.member.dto.MemberProjectResult;
import com.getit.domain.project.member.dto.ProjectSubmitRequest;
import com.getit.domain.project.service.ProjectPublicService;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Group;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.GroupRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/** 부원이 자기 조 명의로 내는 프로젝트. (이슈 #148) */
@SpringBootTest
@Transactional
class ProjectMemberServiceTest {

  @Autowired
  private ProjectMemberService projectMemberService;

  @Autowired
  private ProjectAdminService projectAdminService;

  @Autowired
  private ProjectPublicService projectPublicService;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private FileStorage fileStorage;

  private Generation activeGeneration;
  private User memberInGroup;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);

    Group group = groupRepository.save(Group.create(activeGeneration.getId(), "3조"));
    memberInGroup = member("google-sub-in-group");
    memberInGroup.assignToGroup(group.getId());
    userRepository.flush();
  }

  private User member(String providerId) {
    User user = userRepository.save(
        User.createGuest(providerId, providerId + "@getit.com", "부원", "https://cdn.getit.com/1.png"));
    user.promoteToMember(activeGeneration.getGenerationNo());
    return user;
  }

  private ProjectSubmitRequest request(Long fileId) {
    return new ProjectSubmitRequest(
        "우리 조 프로젝트", "2026-SPRING", "설명", List.of("Java", "Spring"),
        "https://github.com/getit-knu/x", "https://demo.getit.com", fileId);
  }

  private List<String> showcaseTitles() {
    ProjectShowcaseResult showcase = projectPublicService.getShowcase(null, PageRequest.of(0, 10));
    return showcase.content().stream().map(ProjectShowcaseResult.Item::title).toList();
  }

  @Nested
  @DisplayName("submitProject")
  class SubmitProject {

    @Test
    @DisplayName("승인 대기 상태로 들어가고 팀 이름은 내 조 이름이 붙는다")
    void submitsAsPendingWithMyGroupName() {
      MemberProjectResult result =
          projectMemberService.submitProject(memberInGroup.getId(), request(null));

      assertThat(result.status()).isEqualTo(ProjectStatus.PENDING);
      assertThat(result.statusLabel()).isEqualTo("승인 대기");
      // 요청에서 팀 이름을 받으면 남의 조 이름으로 낼 수 있다.
      assertThat(result.teamName()).isEqualTo("3조");
    }

    @Test
    @DisplayName("승인 전에는 공개 쇼케이스에 나오지 않는다")
    void doesNotAppearInShowcaseBeforeApproval() {
      projectMemberService.submitProject(memberInGroup.getId(), request(null));

      assertThat(showcaseTitles()).doesNotContain("우리 조 프로젝트");
    }

    @Test
    @DisplayName("어드민이 승인하면 공개 쇼케이스에 나온다")
    void appearsInShowcaseAfterApproval() {
      MemberProjectResult submitted =
          projectMemberService.submitProject(memberInGroup.getId(), request(null));

      projectAdminService.changeStatus(submitted.id(), ProjectStatus.APPROVED);

      assertThat(showcaseTitles()).contains("우리 조 프로젝트");
    }

    @Test
    @DisplayName("썸네일 파일을 연결한다")
    void connectsThumbnailFile() {
      FileAsset thumbnail = TestStoredFiles.stored(fileAssetRepository, fileStorage,
          "public/thumb", "썸네일.png", "https://cdn/thumb", 1024L, "image/png", 1L);

      projectMemberService.submitProject(memberInGroup.getId(), request(thumbnail.getId()));

      // 연결해 두지 않으면 미연결 파일 정리 배치가 썸네일을 지운다.
      assertThat(fileAssetRepository.findById(thumbnail.getId()).orElseThrow().getStatus())
          .isEqualTo(FileStatus.CONNECTED);
    }

    @Test
    @DisplayName("추천 배치는 부원이 정할 수 없다")
    void memberCannotFeatureOwnProject() {
      MemberProjectResult submitted =
          projectMemberService.submitProject(memberInGroup.getId(), request(null));
      projectAdminService.changeStatus(submitted.id(), ProjectStatus.APPROVED);

      ProjectResult.Item item = projectAdminService
          .getProjects(null, PageRequest.of(0, 10)).content().stream()
          .filter(project -> project.id().equals(submitted.id()))
          .findFirst().orElseThrow();
      assertThat(item.isFeatured()).isFalse();
    }

    @Test
    @DisplayName("조에 배정되지 않았으면 등록할 수 없다")
    void rejectsMemberWithoutGroup() {
      User unassigned = member("google-sub-no-group");
      userRepository.flush();

      assertThatThrownBy(() ->
          projectMemberService.submitProject(unassigned.getId(), request(null)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.NOT_ASSIGNED_TO_GROUP);
    }
  }

  @Nested
  @DisplayName("어드민 승인 · 반려")
  class AdminDecision {

    @Test
    @DisplayName("반려한 것을 다시 승인할 수 있다")
    void canApproveAfterRejecting() {
      MemberProjectResult submitted =
          projectMemberService.submitProject(memberInGroup.getId(), request(null));

      projectAdminService.changeStatus(submitted.id(), ProjectStatus.REJECTED);
      assertThat(showcaseTitles()).doesNotContain("우리 조 프로젝트");

      // 사람이 판단을 바꿀 수 있어야 한다.
      projectAdminService.changeStatus(submitted.id(), ProjectStatus.APPROVED);
      assertThat(showcaseTitles()).contains("우리 조 프로젝트");
    }

    @Test
    @DisplayName("이미 같은 상태면 409 로 막는다")
    void rejectsNoOpTransition() {
      MemberProjectResult submitted =
          projectMemberService.submitProject(memberInGroup.getId(), request(null));
      projectAdminService.changeStatus(submitted.id(), ProjectStatus.APPROVED);

      assertThatThrownBy(() ->
          projectAdminService.changeStatus(submitted.id(), ProjectStatus.APPROVED))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", ProjectErrorCode.PROJECT_STATUS_UNCHANGED);
    }

    @Test
    @DisplayName("어드민이 직접 등록한 프로젝트는 처음부터 공개다")
    void adminCreatedProjectIsApprovedFromTheStart() {
      ProjectResult.Item created = projectAdminService.createProject(
          new ProjectRequest.Write("어드민 프로젝트", "운영진", "2026-SPRING", null, List.of(),
              null, null, null, false, null));

      assertThat(created.status()).isEqualTo(ProjectStatus.APPROVED);
      assertThat(showcaseTitles()).contains("어드민 프로젝트");
    }
  }
}
