package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.LectureFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureFileRepository extends JpaRepository<LectureFile, Long> {

  List<LectureFile> findAllByLectureId(Long lectureId);
}
