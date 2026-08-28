package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LectureQueryServiceImplTest {

  @Autowired
  private LectureQueryService lectureQueryService;

  @Autowired
  private LectureRepository lectureRepository;

  private Long lecture(String title) {
    return lectureRepository.save(Lecture.create(
        1, title, null, null, null, null, true, 1L, null, null, 1L)).getId();
  }

  @Test
  @DisplayName("id 로 제목 맵을 반환한다")
  void returnsTitleMap() {
    Long a = lecture("스프링 기초");
    Long b = lecture("JPA 심화");

    assertThat(lectureQueryService.findTitlesByIds(List.of(a, b)))
        .containsEntry(a, "스프링 기초")
        .containsEntry(b, "JPA 심화");
  }

  @Test
  @DisplayName("빈 입력이면 빈 맵이다")
  void emptyForEmptyInput() {
    assertThat(lectureQueryService.findTitlesByIds(List.of())).isEmpty();
  }

  @Test
  @DisplayName("삭제된 강의는 제외한다")
  void excludesDeletedLecture() {
    Long alive = lecture("살아있음");
    Lecture deleted = lectureRepository.findById(lecture("삭제됨")).orElseThrow();
    deleted.delete();
    lectureRepository.flush();

    assertThat(lectureQueryService.findTitlesByIds(List.of(alive, deleted.getId())))
        .containsOnlyKeys(alive);
  }
}
