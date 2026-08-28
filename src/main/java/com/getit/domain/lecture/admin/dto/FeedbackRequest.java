package com.getit.domain.lecture.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FeedbackRequest {

  public record Write(@NotBlank @Size(max = 2000) String content) { }
}
