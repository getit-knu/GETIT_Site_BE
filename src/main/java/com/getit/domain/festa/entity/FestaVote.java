package com.getit.domain.festa.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 한 사람이 한 팀에 던진 표. 사용자당 한 행이고, 다시 투표하면 projectId 만 바뀐다. (이슈 #232) */
@Entity
@Table(name = "festa_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaVote extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  public static FestaVote of(Long userId, Long projectId) {
    FestaVote vote = new FestaVote();
    vote.userId = userId;
    vote.projectId = projectId;
    return vote;
  }

  public void changeProject(Long projectId) {
    this.projectId = projectId;
  }
}
