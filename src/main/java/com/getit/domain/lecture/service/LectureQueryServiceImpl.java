package com.getit.domain.lecture.service;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LectureQueryServiceImpl implements LectureQueryService {

  private final LectureRepository lectureRepository;

  @Override
  public Map<Long, String> findTitlesByIds(Collection<Long> lectureIds) {
    if (lectureIds.isEmpty()) {
      return Map.of();
    }
    return lectureRepository.findAllById(List.copyOf(lectureIds)).stream()
        .collect(Collectors.toMap(Lecture::getId, Lecture::getTitle));
  }
}
