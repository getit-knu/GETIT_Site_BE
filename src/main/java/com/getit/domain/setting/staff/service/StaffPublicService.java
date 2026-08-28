package com.getit.domain.setting.staff.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.setting.staff.dto.PublicStaffResult;
import com.getit.domain.setting.staff.dto.StaffDirectoryResult;
import com.getit.domain.setting.staff.dto.StaffSectionGroup;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.repository.StaffRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진 소개(공개). (API 명세서 2.3)
 *
 * <p>활성 기수가 없으면 3개 section 모두 빈 배열로 반환한다 — 공개 페이지는
 * {@code StaffAdminService} 처럼 404 로 실패하지 않아야 한다(2.8 의 CLOSED 처리와 같은 이유).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffPublicService {

  private final StaffRepository staffRepository;
  private final GenerationQueryService generationQueryService;
  private final FileQueryService fileQueryService;

  public StaffDirectoryResult getStaffDirectory() {
    List<Staff> staffs = generationQueryService.findActive()
        .map(active -> staffRepository.findByGenerationNoOrderBySectionAscOrderAscIdAsc(active.generationNo()))
        .orElseGet(List::of);

    Map<Long, String> profileImageUrls = findProfileImageUrls(staffs);
    Map<StaffSection, List<Staff>> staffsBySection = staffs.stream()
        .collect(Collectors.groupingBy(Staff::getSection));

    List<StaffSectionGroup> sections = Arrays.stream(StaffSection.values())
        .map(section -> StaffSectionGroup.of(section, toResults(staffsBySection.get(section), profileImageUrls)))
        .toList();

    return new StaffDirectoryResult(sections);
  }

  private List<PublicStaffResult> toResults(List<Staff> staffs, Map<Long, String> profileImageUrls) {
    if (staffs == null) {
      return List.of();
    }
    return staffs.stream()
        .map(staff -> PublicStaffResult.of(staff, profileImageUrls.get(staff.getFileId())))
        .toList();
  }

  /**
   * 후보 전체의 fileId 를 모아 한 번에 조회한다({@code StaffAdminService.findProfileImageUrls}
   * 와 동일한 이유). {@code Map.of()} 는 null 키 조회에서 NPE 를 던지므로 fileId 가 없는 운영진을
   * 위해 {@link Collections#emptyMap()} 을 쓴다.
   */
  private Map<Long, String> findProfileImageUrls(List<Staff> staffs) {
    List<Long> fileIds = staffs.stream().map(Staff::getFileId).filter(Objects::nonNull).distinct().toList();
    if (fileIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));
  }
}
