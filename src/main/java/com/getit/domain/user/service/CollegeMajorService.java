package com.getit.domain.user.service;

import com.getit.domain.user.dto.CollegeResult;
import com.getit.domain.user.dto.MajorResult;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 단과대학 · 전공 목록 조회. (API 명세서 2.6 · 2.7) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CollegeMajorService {

  private final CollegeRepository collegeRepository;
  private final MajorRepository majorRepository;

  public List<CollegeResult> getColleges() {
    return collegeRepository.findAllByOrderByIdAsc().stream()
        .map(CollegeResult::from)
        .toList();
  }

  /** collegeId 가 없으면 전체 전공을 반환한다. (API 명세서 2.7) */
  public List<MajorResult> getMajors(Long collegeId) {
    List<Major> majors = collegeId != null
        ? majorRepository.findByCollegeIdOrderByIdAsc(collegeId)
        : majorRepository.findAllByOrderByIdAsc();

    return majors.stream()
        .map(MajorResult::from)
        .toList();
  }
}
