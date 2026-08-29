package com.getit.domain.setting.home.dto;

import com.getit.domain.recruitment.entity.RecruitmentPhase;
import java.time.OffsetDateTime;
import java.util.List;

/** 홈 통합 조회 결과. (API 명세서 2.1) */
public record HomeResult(
    GenerationInfo generation,
    RecruitmentInfo recruitment,
    List<CurriculumInfo> curriculums,
    List<FeaturedProjectInfo> featuredProjects,
    List<FaqInfo> faqs,
    FeaturesInfo features
) {

  /** 활성 기수가 없으면 null 이다 — 공개 API 라 404 로 실패하지 않는다(2.8과 동일 원칙). */
  public record GenerationInfo(Integer generationNo, Integer year) { }

  /** {@code RecruitmentStatusResult} 를 그대로 재사용한다(2.8과 phase/D-day/메시지 계산 공유). */
  public record RecruitmentInfo(
      RecruitmentPhase phase,
      Long dDay,
      String message,
      boolean applyEnabled,
      OffsetDateTime totalStartAt,
      OffsetDateTime totalEndAt
  ) { }

  public record CurriculumInfo(Long id, Integer order, String title, String subtitle) { }

  /** {@code thumbnailUrl} 은 {@code fileId} 를 {@code FileQueryService} 로 해석한 결과다. */
  public record FeaturedProjectInfo(Long id, String title, String description, String thumbnailUrl) { }

  public record FaqInfo(Long id, String question, String answer) { }

  public record FeaturesInfo(boolean stockGame, boolean mockInvestment) { }
}
