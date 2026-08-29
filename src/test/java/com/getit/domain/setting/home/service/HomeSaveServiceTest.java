package com.getit.domain.setting.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.exception.EventErrorCode;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.home.dto.HomeSaveRequest;
import com.getit.domain.setting.home.dto.HomeSaveResult;
import com.getit.domain.setting.home.exception.HomeErrorCode;
import com.getit.global.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HomeSaveServiceTest {

  @Autowired
  private HomeSaveService homeSaveService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Autowired
  private EventRepository eventRepository;

  @Autowired
  private FaqRepository faqRepository;

  private Generation activeGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    return generationRepository.save(generation);
  }

  private HomeSaveRequest requestFor(Generation generation) {
    LocalDateTime now = LocalDateTime.now();
    return new HomeSaveRequest(
        new HomeSaveRequest.GenerationInfo(generation.getGenerationNo(), generation.getYear()),
        new HomeSaveRequest.ScheduleInfo(
            now.minusDays(1), now.plusDays(10), now.minusDays(1), now.plusDays(5), now.plusDays(6)),
        List.of(),
        List.of(new HomeSaveRequest.CurriculumInfo(null, "Python & 데이터 분석", "부제")),
        List.of(new HomeSaveRequest.EventInfo(
            null, "GETIT Chat", "IT5호관", LocalDate.now().plusDays(1), LocalDate.now().plusDays(1),
            EventType.EVENT, true)),
        List.of(new HomeSaveRequest.FaqInfo(null, "동아리 활동 시간은?", "매주 화요일", true)));
  }

  @Test
  @DisplayName("일정 · 커리큘럼 · 분류 · 행사 · FAQ 를 한 트랜잭션으로 저장한다")
  void savesEverythingInOneTransaction() {
    Generation generation = activeGeneration();

    HomeSaveResult result = homeSaveService.save(requestFor(generation), false);

    assertThat(result.generationNo()).isEqualTo(9);
    assertThat(recruitmentScheduleRepository.findByGenerationId(generation.getId())).isPresent();
    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(generation.getId()))
        .extracting(Curriculum::getTitle).containsExactly("Python & 데이터 분석");
    assertThat(eventRepository.findByGenerationIdOrderByStartDateAscIdAsc(generation.getId())).hasSize(1);
    assertThat(faqRepository.findAllByOrderByOrderAscIdAsc()).hasSize(1);
  }

  @Test
  @DisplayName("요청의 generationNo 가 활성 기수와 다르면 예외가 발생하고 아무것도 저장되지 않는다")
  void throwsWhenGenerationMismatch() {
    Generation generation = activeGeneration();
    HomeSaveRequest request = new HomeSaveRequest(
        new HomeSaveRequest.GenerationInfo(999, 2026),
        requestFor(generation).schedule(),
        List.of(), List.of(), List.of(), List.of());

    assertThatThrownBy(() -> homeSaveService.save(request, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(HomeErrorCode.GENERATION_NOT_ACTIVE);
    assertThat(curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(generation.getId())).isEmpty();
  }

  @Test
  @DisplayName("활성 기수가 없으면 예외가 발생한다")
  void throwsWhenNoActiveGeneration() {
    HomeSaveRequest request = new HomeSaveRequest(
        new HomeSaveRequest.GenerationInfo(9, 2026),
        new HomeSaveRequest.ScheduleInfo(
            LocalDateTime.now(), LocalDateTime.now().plusDays(1),
            LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()),
        List.of(), List.of(), List.of(), List.of());

    assertThatThrownBy(() -> homeSaveService.save(request, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(HomeErrorCode.ACTIVE_GENERATION_NOT_FOUND);
  }

  @Test
  @DisplayName("B 계약 하나가 실패하면(예: 존재하지 않는 이벤트 id) 예외가 그대로 전파돼 트랜잭션이 실패한다")
  void propagatesBulkContractFailure() {
    // 실제 DB 롤백 여부는 여기서 재확인하지 않는다 — 이 테스트 자체가 @Transactional 로 감싸여
    // 있어, 예외 발생 후에도 같은 트랜잭션 안에서는 방금 저장한 행이 그대로 조회된다(진짜
    // 롤백은 테스트 메서드가 끝날 때 스프링이 처리). 여기서 검증할 것은 A 가 B 계약의 예외를
    // 삼키지 않고 그대로 전파해서 트랜잭션 전체가 실패로 끝나는지다 — 실제 롤백 보장은 Spring
    // @Transactional 기본 전파 규칙(RuntimeException 시 rollback-only) 자체의 몫이다.
    Generation generation = activeGeneration();
    HomeSaveRequest base = requestFor(generation);
    HomeSaveRequest broken = new HomeSaveRequest(
        base.generation(), base.schedule(), base.tracks(), base.curriculums(),
        List.of(new HomeSaveRequest.EventInfo(
            999L, "없는 행사", "IT5호관", LocalDate.now(), LocalDate.now(), EventType.EVENT, true)),
        base.faqs());

    assertThatThrownBy(() -> homeSaveService.save(broken, false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(EventErrorCode.EVENT_NOT_FOUND);
  }
}
