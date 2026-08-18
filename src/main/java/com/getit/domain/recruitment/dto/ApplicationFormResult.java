package com.getit.domain.recruitment.dto;

import com.getit.domain.recruitment.entity.RecruitmentPhase;
import java.time.LocalDateTime;
import java.util.List;

/** 지원서 양식 조회 결과. (API 명세서 3.1) */
public record ApplicationFormResult(
    Integer generationNo,
    RecruitmentPhase phase,
    LocalDateTime deadline,
    BasicInfo basicInfoPrefill,
    List<ApplicationFormQuestion> questions
) { }
