package com.getit.domain.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberQuestionRequest {

  public record Create(@NotBlank @Size(max = 2000) String content) { }
}
