package com.getit.domain.setting.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryRequest {

  public record TrackCreate(@NotBlank String name) { }

  public record TrackUpdate(@NotBlank String name, Integer order) { }

  public record SubCategoryCreate(@NotNull Long trackId, @NotBlank String name) { }

  public record SubCategoryUpdate(@NotBlank String name, Integer order) { }
}
