package com.getit.domain.qna.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "answer",
    uniqueConstraints = @UniqueConstraint(name = "uk_answer_question_id", columnNames = "question_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "question_id", nullable = false)
  private long questionId;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(name = "admin_id", nullable = false)
  private long adminId;

  @Builder(access = AccessLevel.PRIVATE)
  private Answer(String content, long questionId, long adminId) {
    this.content = content;
    this.questionId = questionId;
    this.adminId = adminId;
  }

  public static Answer create(long questionId, long adminId, String content) {
    return Answer.builder()
        .content(content)
        .questionId(questionId)
        .adminId(adminId)
        .build();
  }

  public void update(String content) { this.content = content; }

  public boolean isWrittenBy(long adminId) { return this.adminId == adminId; }
}
