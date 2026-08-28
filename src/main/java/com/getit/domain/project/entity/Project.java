package com.getit.domain.project.entity;

import com.getit.domain.project.dto.ProjectCommand;
import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
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

  @Builder(access = AccessLevel.PRIVATE)
  private Project(ProjectCommand command, int order) {
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
  }

  public static Project create(ProjectCommand command, int order) {
    return Project.builder().command(command).order(order).build();
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
}
