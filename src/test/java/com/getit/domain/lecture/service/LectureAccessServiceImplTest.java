package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LectureAccessServiceImplTest {

  private static final int ACTIVE_GENERATION_NO = 9;

  @Autowired
  private LectureAccessService lectureAccessService;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private UserRepository userRepository;

  private Long activeGenerationId;
  private Long memberId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(ACTIVE_GENERATION_NO, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();
    User member = User.createGuest("m", "m@getit.com", "부원", null);
    member.promoteToMember(ACTIVE_GENERATION_NO);
    memberId = userRepository.save(member).getId();
  }

  private Long lecture(boolean published, Long generationId) {
    return lectureRepository.save(Lecture.create(
        1, "1주차", null, null, null, null, published, generationId, null, null, 1L)).getId();
  }

  @Test
  @DisplayName("활성 부원 + 공개 강의면 통과한다")
  void passesForActiveMemberAndPublishedLecture() {
    Long lectureId = lecture(true, activeGenerationId);

    assertThatCode(() -> lectureAccessService.requireVisibleToMember(lectureId, memberId))
        .doesNotThrowAnyException();
  }

  @Test
  @DisplayName("GUEST 면 403")
  void forbidsGuest() {
    Long lectureId = lecture(true, activeGenerationId);
    Long guestId = userRepository.save(User.createGuest("g", "g@getit.com", "게스트", null)).getId();

    assertThatThrownBy(() -> lectureAccessService.requireVisibleToMember(lectureId, guestId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("비공개 강의면 404")
  void notFoundForUnpublished() {
    Long lectureId = lecture(false, activeGenerationId);

    assertThatThrownBy(() -> lectureAccessService.requireVisibleToMember(lectureId, memberId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(LectureErrorCode.LECTURE_NOT_FOUND);
  }

  @Test
  @DisplayName("없는 강의면 404")
  void notFoundForMissingLecture() {
    assertThatThrownBy(() -> lectureAccessService.requireVisibleToMember(999L, memberId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(LectureErrorCode.LECTURE_NOT_FOUND);
  }
}
