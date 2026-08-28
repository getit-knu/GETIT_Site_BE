package com.getit.domain.qna.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "question")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2000)
  private String content;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private QnaStatus status;

  @Column(name = "author_id", nullable = false)
  private long authorId;

  @Column(name = "lecture_id")
  private Long lectureId;

  @Builder(access = AccessLevel.PRIVATE)
  private Question(String content, long authorId, Long lectureId) {
    this.content = content;
    this.status = QnaStatus.PENDING;
    this.authorId = authorId;
    this.lectureId = lectureId;
  }

  public static Question create(long authorId, Long lectureId, String content) {
    return Question.builder()
        .content(content)
        .authorId(authorId)
        .lectureId(lectureId)
        .build();
  }

  public void markAnswered() { this.status = QnaStatus.ANSWERED; }

  public boolean isWrittenBy(long userId) { return this.authorId == userId; }
}
