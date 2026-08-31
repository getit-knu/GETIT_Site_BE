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

  @Column(name = "team_name", nullable = false, length = 100)
  private String teamName;

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

  /** 공개 상태. 부원이 낸 것은 승인을 거쳐야 공개된다 (이슈 #148). */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectStatus status;

  @Builder(access = AccessLevel.PRIVATE)
  private Project(ProjectCommand command, int order, ProjectStatus status) {
    this.title = command.title();
    this.teamName = command.teamName();
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
  public static Project submit(ProjectCommand command, int order) {
    return Project.builder().command(command).order(order).status(ProjectStatus.PENDING).build();
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
  public void changeStatus(ProjectStatus status) { this.status = status; }
}
