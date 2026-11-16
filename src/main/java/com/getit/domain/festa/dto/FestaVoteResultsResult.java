package com.getit.domain.festa.dto;

import java.util.List;

/** 어드민이 보는 득표 집계. (이슈 #232) */
public record FestaVoteResultsResult(long voterCount, List<ProjectResult> projects) {

  public record ProjectResult(Long projectId, String title, String teamName, long voteCount) { }
}
