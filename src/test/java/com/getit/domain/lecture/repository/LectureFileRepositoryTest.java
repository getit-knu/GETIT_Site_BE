package com.getit.domain.lecture.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.lecture.entity.LectureFile;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class LectureFileRepositoryTest {

  @Autowired
  private LectureFileRepository lectureFileRepository;

  @Test
  @DisplayName("등록된 순서(id 오름차순)로 반환한다")
  void findsAllOrderedById() {
    lectureFileRepository.save(LectureFile.create("2주차자료.pdf", 1L, 502L));
    lectureFileRepository.save(LectureFile.create("1주차자료.pdf", 1L, 501L));

    assertThat(lectureFileRepository.findAllByLectureIdOrderByIdAsc(1L))
        .extracting(LectureFile::getDisplayName)
        .containsExactly("2주차자료.pdf", "1주차자료.pdf");
  }
}
