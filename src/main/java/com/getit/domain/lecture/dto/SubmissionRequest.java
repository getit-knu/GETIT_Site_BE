package com.getit.domain.lecture.dto;

import jakarta.validation.constraints.Size;

public class SubmissionRequest {

  public record Submit(
      Long fileId, @Size(max = 512) String linkUrl, @Size(max = 2000) String comment
  ) { }
}
