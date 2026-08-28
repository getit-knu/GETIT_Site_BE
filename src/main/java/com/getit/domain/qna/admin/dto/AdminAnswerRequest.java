package com.getit.domain.qna.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminAnswerRequest {

  public record Write(@NotBlank @Size(max = 2000) String content) { }
}
