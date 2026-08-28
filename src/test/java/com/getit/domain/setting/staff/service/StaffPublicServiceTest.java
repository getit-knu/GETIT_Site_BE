package com.getit.domain.setting.staff.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.staff.dto.StaffDirectoryResult;
import com.getit.domain.setting.staff.dto.StaffSectionGroup;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StaffPublicServiceTest {

  @Autowired
  private StaffPublicService staffPublicService;

  @Autowired
  private StaffRepository staffRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Test
  @DisplayName("항상 3개 section 을 반환하고, 소속 운영진이 없으면 빈 배열이다")
  void alwaysReturnsThreeSections() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    staffRepository.save(
        Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null));

    StaffDirectoryResult result = staffPublicService.getStaffDirectory();

    assertThat(result.sections()).extracting(StaffSectionGroup::section)
        .containsExactly(StaffSection.EXECUTIVE, StaffSection.SW, StaffSection.STARTUP);
    assertThat(result.sections()).extracting(StaffSectionGroup::sectionName)
        .containsExactly("회장단", "SW 운영진", "창업 운영진");
    assertThat(result.sections().get(0).staffs()).hasSize(1);
    assertThat(result.sections().get(1).staffs()).isEmpty();
    assertThat(result.sections().get(2).staffs()).isEmpty();
  }

  @Test
  @DisplayName("활성 기수가 없으면 3개 section 모두 빈 배열이다")
  void returnsEmptySectionsWhenNoActiveGeneration() {
    StaffDirectoryResult result = staffPublicService.getStaffDirectory();

    assertThat(result.sections()).hasSize(3);
    assertThat(result.sections()).allSatisfy(section -> assertThat(section.staffs()).isEmpty());
  }

  @Test
  @DisplayName("introduction 이 없으면 기본 문구로 채우고, fileId 가 있으면 profileImageUrl 을 채운다")
  void fillsDefaultsAndProfileImageUrl() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    FileAsset uploaded = fileAssetRepository.save(
        FileAsset.upload("staff-1", "staff-1.png", "https://cdn.getit.com/staff-1", 100L, "image/png", 1L));
    staffRepository.save(Staff.create(
        9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, uploaded.getId()));

    StaffDirectoryResult result = staffPublicService.getStaffDirectory();

    var swStaff = result.sections().get(1).staffs().get(0);
    assertThat(swStaff.introduction()).isEqualTo("한줄 소개를 작성해주세요");
    assertThat(swStaff.profileImageUrl()).isEqualTo(uploaded.getUrl());
  }

  @Test
  @DisplayName("다른 기수(비활성)의 운영진은 노출되지 않는다")
  void excludesStaffFromInactiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    staffRepository.save(
        Staff.create(8, 1, StaffSection.SW, "SW 운영진", "지난기수", "컴퓨터공학과", null, null, null));

    StaffDirectoryResult result = staffPublicService.getStaffDirectory();

    assertThat(result.sections().get(1).staffs()).isEmpty();
  }
}
