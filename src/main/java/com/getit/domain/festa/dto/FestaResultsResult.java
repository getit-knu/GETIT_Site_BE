package com.getit.domain.festa.dto;

import java.util.List;

/** 어드민이 보는 투자 집계. 행사 중에는 공개하지 않는다. (이슈 #222) */
public record FestaResultsResult(long investorCount, List<ProjectResult> projects) {

  public record ProjectResult(
      Long projectId,
      String title,
      String teamName,
      long totalAmount,
      long investorCount
  ) { }
}
