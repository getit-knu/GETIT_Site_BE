package com.getit.domain.setting.category.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

  public record TrackCreate(
      @NotBlank @Size(max = 50) String name
  ) { }

  public record TrackUpdate(
      @NotBlank @Size(max = 50) String name,
      @Min(1) Integer order
  ) { }

  public record SubCategoryCreate(
      @NotNull Long trackId,
      @NotBlank @Size(max = 50) String name
  ) { }

  public record SubCategoryUpdate(
      @NotBlank @Size(max = 50) String name,
      @Min(1) Integer order
  ) { }
}
