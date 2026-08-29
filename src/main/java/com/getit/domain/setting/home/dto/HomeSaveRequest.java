package com.getit.domain.setting.home.dto;

import com.getit.domain.setting.event.entity.EventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 홈 화면 일괄 저장 요청. (API 명세서 10.20)
 *
 * <p>{@code events[].isVisible}·{@code faqs[].isVisible}는 명세서 예시엔 없지만, 소비하는
 * {@code EventUpsert}·{@code FaqUpsert}(B, 이슈 #133)가 항상 명시적으로 받도록 설계돼 있어
 * 여기서도 필수로 받는다 — B PR 리뷰 포인트에 이미 정리돼 있다.
 */
public record HomeSaveRequest(
    @Valid @NotNull GenerationInfo generation,
    @Valid @NotNull ScheduleInfo schedule,
    @NotNull List<@NotNull @Valid TrackInfo> tracks,
    @NotNull List<@NotNull @Valid CurriculumInfo> curriculums,
    @NotNull List<@NotNull @Valid EventInfo> events,
    @NotNull List<@NotNull @Valid FaqInfo> faqs
) {

  public record GenerationInfo(
      @NotNull @Positive Integer generationNo,
      @NotNull @Positive Integer year
  ) { }

  public record ScheduleInfo(
      @NotNull LocalDateTime totalStartAt,
      @NotNull LocalDateTime totalEndAt,
      @NotNull LocalDateTime documentStartAt,
      @NotNull LocalDateTime documentEndAt,
      @NotNull LocalDateTime interviewStartAt
  ) { }

  public record TrackInfo(
      Long id,
      @NotBlank @Size(max = 50) String name,
      @NotNull List<@NotNull @Valid SubCategoryInfo> subCategories
  ) { }

  public record SubCategoryInfo(
      Long id,
      @NotBlank @Size(max = 50) String name
  ) { }

  public record CurriculumInfo(
      Long id,
      @NotBlank @Size(max = 100) String title,
      @NotBlank @Size(max = 255) String subtitle
  ) { }

  public record EventInfo(
      Long id,
      @NotBlank @Size(max = 100) String title,
      @NotBlank @Size(max = 100) String place,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      @NotNull EventType type,
      @NotNull Boolean isVisible
  ) { }

  public record FaqInfo(
      Long id,
      @NotBlank @Size(max = 255) String question,
      @NotBlank @Size(max = 2000) String answer,
      @NotNull Boolean isVisible
  ) { }
}
