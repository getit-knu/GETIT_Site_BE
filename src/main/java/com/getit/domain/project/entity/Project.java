package com.getit.domain.project.entity;

import com.getit.domain.project.dto.ProjectCommand;
import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "project")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String title;

  /** 화면에 보여줄 팀 이름. 소유 판정에는 쓰지 않는다 — {@link #groupId} 를 본다. */
  @Column(name = "team_name", nullable = false, length = 100)
  private String teamName;

  /**
   * 낸 조. 어드민이 직접 등록한 프로젝트는 {@code null} 이다. (PR #197 리뷰 지적)
   *
   * <p>조 이름은 기수 안에서만 유일하고(uk_user_group_generation_name) 어드민이 바꿀 수도
   * 있어 소유 판정에 쓸 수 없다. 이름으로 찾으면 같은 이름을 쓴 지난 기수 조의 것이 섞이고,
   * 이름을 바꾸면 그 조가 낸 것이 통째로 사라진다.
   */
  @Column(name = "group_id")
  private Long groupId;

  @Column(nullable = false, length = 50)
  private String semester;

  @Column(columnDefinition = "TEXT", nullable = true)
  private String description;

  @Convert(converter = TechStackListConverter.class)
  @Column(name = "tech_stacks", length = 500, nullable = true)
  private List<String> techStacks;

  @Column(name = "code_url", length = 512, nullable = true)
  private String codeUrl;

  @Column(name = "demo_url", length = 512, nullable = true)
  private String demoUrl;

  @Column(name = "is_featured", nullable = false)
  private boolean isFeatured;

  @Column(name = "project_order", nullable = false)
  private int order;

  @Column(name = "file_id", nullable = true)
  private Long fileId;

  /**
   * 공개 상태. 부원이 낸 것은 승인을 거쳐야 공개된다 (이슈 #148).
   *
   * <p>JDBC 타입을 VARCHAR 로 못 박는다. Hibernate 6 의 MySQL 매핑은
   * {@code EnumType.STRING} 을 네이티브 ENUM 으로 잡을 수 있는데, 그러면 운영의
   * {@code ddl-auto: validate} 가 마이그레이션의 varchar(20) 과 어긋난다
   * (PR #165 리뷰 지적). {@code User.role} 과 같은 방식이다.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private ProjectStatus status;

  @Builder(access = AccessLevel.PRIVATE)
  private Project(ProjectCommand command, int order, ProjectStatus status, Long groupId) {
    this.title = command.title();
    this.teamName = command.teamName();
    this.groupId = groupId;
    this.semester = command.semester();
    this.description = command.description();
    this.techStacks = command.techStacks();
    this.codeUrl = command.codeUrl();
    this.demoUrl = command.demoUrl();
    this.isFeatured = command.isFeatured();
    this.order = order;
    this.fileId = command.fileId();
    this.status = status;
  }

  /** 어드민이 직접 등록한다. 자기가 올린 것을 자기가 승인할 이유가 없어 바로 공개다. */
  public static Project create(ProjectCommand command, int order) {
    return Project.builder().command(command).order(order).status(ProjectStatus.APPROVED).build();
  }

  /** 부원이 자기 조 명의로 낸다. 어드민이 승인해야 공개된다 (이슈 #148). */
  public static Project submit(ProjectCommand command, int order, Long groupId) {
    return Project.builder()
        .command(command).order(order).status(ProjectStatus.PENDING).groupId(groupId).build();
  }

  public void update(ProjectCommand command) {
    this.title = command.title();
    this.teamName = command.teamName();
    this.semester = command.semester();
    this.description = command.description();
    this.techStacks = command.techStacks();
    this.codeUrl = command.codeUrl();
    this.demoUrl = command.demoUrl();
    this.isFeatured = command.isFeatured();
    this.fileId = command.fileId();
  }

  public void updateOrder(int order) { this.order = order; }

  /** 반려한 것을 다시 승인할 수도 있으므로 PENDING 에서만 오는 것으로 보지 않는다. */
  /**
   * 반려 사유. 반려 상태일 때만 있다. (이슈 #190)
   *
   * <p>다시 승인하면 비운다. 남겨 두면 공개된 프로젝트에 반려 사유가 붙어 있는 이상한
   * 상태가 된다.
   */
  @Column(length = 500)
  private String rejectReason;

  /**
   * 승인 · 대기로 바꾼다. 반려 사유는 비운다.
   *
   * <p>반려는 받지 않는다. 여기로 반려하면 사유 없는 반려가 만들어져, 부원이 이유를 알게
   * 하려던 것이 그대로 뚫린다 (PR #197 리뷰 지적). 반려는 {@link #reject} 로만 한다.
   */
  public void changeStatus(ProjectStatus status) {
    if (status == ProjectStatus.REJECTED) {
      throw new IllegalArgumentException("반려는 사유와 함께 reject 로 처리한다");
    }
    this.status = status;
    this.rejectReason = null;
  }

  /** 사유를 남기며 반려한다. */
  public void reject(String reason) {
    this.status = ProjectStatus.REJECTED;
    this.rejectReason = reason;
  }
}
