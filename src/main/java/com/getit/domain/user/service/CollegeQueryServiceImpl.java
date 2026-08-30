package com.getit.domain.user.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.user.entity.College;
import com.getit.domain.user.repository.CollegeRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CollegeQueryServiceImpl implements CollegeQueryService {

  private final CollegeRepository collegeRepository;

  @Override
  public Map<Long, String> findNamesByIds(Collection<Long> collegeIds) {
    if (collegeIds.isEmpty()) {
      return Map.of();
    }
    return collegeRepository.findAllById(collegeIds).stream()
        .collect(Collectors.toMap(College::getId, College::getName));
  }
}
