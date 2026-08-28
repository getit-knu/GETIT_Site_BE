package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.LectureFile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureFileRepository extends JpaRepository<LectureFile, Long> {

  List<LectureFile> findAllByLectureIdOrderByIdAsc(Long lectureId);

  Optional<LectureFile> findByLectureIdAndFileId(Long lectureId, Long fileId);
}
