package com.getit.domain.setting.staff.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.staff.dto.StaffResult;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.exception.StaffErrorCode;
import com.getit.domain.setting.staff.repository.StaffRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StaffAdminServiceTest {

  @Autowired
  private StaffAdminService staffAdminService;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private FileAsset file(String key) {
    return fileAssetRepository.save(
        FileAsset.upload(key, key + ".png", "https://cdn.getit.com/" + key, 100L, "image/png", 1L));
  }

  @Nested
  @DisplayName("getStaffs")
  class GetStaffs {

    @Test
    @DisplayName("활성 기수의 운영진을 section → order 순으로 반환한다")
    void returnsStaffsInOrder() {
      staffRepository.save(
          Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null));
      staffRepository.save(
          Staff.create(9, 2, StaffSection.SW, "SW 운영진", "이영희", "전자공학과 19", null, null, null));
      staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));

      List<StaffResult> results = staffAdminService.getStaffs();

      assertThat(results).extracting(StaffResult::name).containsExactly("김철수", "홍길동", "이영희");
    }

    @Test
    @DisplayName("introduction 이 없으면 기본 문구로 채운다")
    void fillsDefaultIntroductionWhenMissing() {
      staffRepository.save(
          Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null));

      List<StaffResult> results = staffAdminService.getStaffs();

      assertThat(results.get(0).introduction()).isEqualTo("한줄 소개를 작성해주세요");
    }

    @Test
    @DisplayName("fileId 가 있으면 profileImageUrl 을 채운다")
    void resolvesProfileImageUrl() {
      FileAsset uploaded = file("staff-1");
      staffRepository.save(Staff.create(
          9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, uploaded.getId()));

      List<StaffResult> results = staffAdminService.getStaffs();

      assertThat(results.get(0).profileImageUrl()).isEqualTo(uploaded.getUrl());
    }
  }

  @Nested
  @DisplayName("createStaff")
  class CreateStaff {

    @Test
    @DisplayName("운영진을 추가하고 order 를 자동으로 부여한다")
    void createsStaffWithAutoAssignedOrder() {
      staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));

      StaffResult saved = staffAdminService.createStaff(
          null, "이영희", "SW 운영진", StaffSection.SW, "전자공학과 19", "안녕하세요", null, 9);

      assertThat(saved.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("요청 generationNo 가 활성 기수와 다르면 예외가 발생한다")
    void throwsWhenGenerationMismatch() {
      assertThatThrownBy(() -> staffAdminService.createStaff(
          null, "이영희", "SW 운영진", StaffSection.SW, "전자공학과 19", null, null, 999))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(StaffErrorCode.GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 fileId 면 예외가 발생한다")
    void throwsWhenFileNotFound() {
      assertThatThrownBy(() -> staffAdminService.createStaff(
          null, "이영희", "SW 운영진", StaffSection.SW, "전자공학과 19", null, 999L, 9))
          .isInstanceOf(BusinessException.class);
    }
  }

  @Nested
  @DisplayName("updateStaff")
  class UpdateStaff {

    @Test
    @DisplayName("section 이 그대로면 order 를 바꾸지 않는다")
    void keepsOrderWhenSectionUnchanged() {
      Staff staff = staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));

      StaffResult updated = staffAdminService.updateStaff(
          staff.getId(), null, "홍길동", "부운영진", StaffSection.SW, "컴퓨터공학과 21", null, null, 9);

      assertThat(updated.order()).isEqualTo(1);
      assertThat(updated.staffRole()).isEqualTo("부운영진");
    }

    @Test
    @DisplayName("section 이 바뀌면 새 section 의 마지막 순번으로 재배정한다")
    void reassignsOrderWhenSectionChanges() {
      staffRepository.save(
          Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null));
      Staff moving = staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));

      StaffResult updated = staffAdminService.updateStaff(
          moving.getId(), null, "홍길동", "부회장", StaffSection.EXECUTIVE, "컴퓨터공학과 21", null, null, 9);

      assertThat(updated.section()).isEqualTo(StaffSection.EXECUTIVE);
      assertThat(updated.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("다른 기수의 운영진이면 예외가 발생한다")
    void throwsWhenBelongsToOtherGeneration() {
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2025));
      Staff other = staffRepository.save(
          Staff.create(8, 1, StaffSection.SW, "SW 운영진", "지난기수", "컴퓨터공학과", null, null, null));
      assertThat(otherGeneration).isNotNull();

      assertThatThrownBy(() -> staffAdminService.updateStaff(
          other.getId(), null, "지난기수", "SW 운영진", StaffSection.SW, "컴퓨터공학과", null, null, 9))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(StaffErrorCode.STAFF_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteStaff")
  class DeleteStaff {

    @Test
    @DisplayName("삭제하고 뒤 순번을 한 칸씩 당긴다")
    void deletesAndShiftsRemainingOrder() {
      Staff first = staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));
      Staff second = staffRepository.save(
          Staff.create(9, 2, StaffSection.SW, "SW 운영진", "이영희", "전자공학과 19", null, null, null));

      staffAdminService.deleteStaff(first.getId());

      assertThat(staffRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("reorderStaffs")
  class ReorderStaffs {

    @Test
    @DisplayName("section 안에서 배열 순서대로 order 를 재부여한다")
    void reordersWithinSection() {
      Staff first = staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));
      Staff second = staffRepository.save(
          Staff.create(9, 2, StaffSection.SW, "SW 운영진", "이영희", "전자공학과 19", null, null, null));

      staffAdminService.reorderStaffs(StaffSection.SW, List.of(second.getId(), first.getId()));

      assertThat(staffRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(1);
      assertThat(staffRepository.findById(first.getId()).orElseThrow().getOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("section 소속 일부만 보내면 예외가 발생한다")
    void throwsWhenNotExhaustive() {
      Staff first = staffRepository.save(
          Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null));
      staffRepository.save(
          Staff.create(9, 2, StaffSection.SW, "SW 운영진", "이영희", "전자공학과 19", null, null, null));

      assertThatThrownBy(() -> staffAdminService.reorderStaffs(StaffSection.SW, List.of(first.getId())))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }
  }
}
