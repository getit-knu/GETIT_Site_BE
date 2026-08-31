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
  /**
   * 반려 사유. 반려 상태일 때만 있다. (이슈 #190)
   *
   * <p>다시 승인하면 비운다. 남겨 두면 공개된 프로젝트에 반려 사유가 붙어 있는 이상한
   * 상태가 된다.
   */
  @Column(length = 500)
  private String rejectReason;

  /** 승인 · 대기로 되돌린다. 반려 사유는 비운다. */
  public void changeStatus(ProjectStatus status) {
    this.status = status;
    this.rejectReason = null;
  }

  /** 사유를 남기며 반려한다. */
  public void reject(String reason) {
    this.status = ProjectStatus.REJECTED;
    this.rejectReason = reason;
  }
}
