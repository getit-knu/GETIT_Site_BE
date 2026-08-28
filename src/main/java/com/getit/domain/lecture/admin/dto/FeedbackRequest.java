package com.getit.domain.lecture.admin.dto;

import jakarta.validation.constraints.NotBlank;

public class FeedbackRequest {

  public record Write(@NotBlank String content) { }
}
