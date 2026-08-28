package com.getit.domain.lecture.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(name = "submission_id", nullable = false)
  private long submissionId;

  @Column(name = "admin_id", nullable = false)
  private long adminId;

  @Builder(access = AccessLevel.PRIVATE)
  private Feedback(String content, long submissionId, long adminId) {
    this.content = content;
    this.submissionId = submissionId;
    this.adminId = adminId;
  }

  public static Feedback create(long submissionId, long adminId, String content) {
    return Feedback.builder()
        .submissionId(submissionId)
        .adminId(adminId)
        .content(content)
        .build();
  }

  public void update(String content) { this.content = content; }

  public boolean isWrittenBy(long adminId) { return this.adminId == adminId; }
}
