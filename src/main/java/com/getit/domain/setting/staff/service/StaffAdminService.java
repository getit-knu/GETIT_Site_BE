package com.getit.domain.setting.staff.service;

import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.setting.staff.dto.StaffRequest;
import com.getit.domain.setting.staff.dto.StaffResult;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.exception.StaffErrorCode;
import com.getit.domain.setting.staff.repository.StaffRepository;
import com.getit.global.exception.BusinessException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 운영진 프로필 조회 · 저장 · 순서 변경. (API 명세서 10.21 · 10.22)
 *
 * <p>전부 활성 기수로 스코프한다({@code CurriculumAdminService}와 동일한 이유 — 운영자 화면에
 * 노출되는 콘텐츠라 항상 "지금 진행 중인 기수" 기준이어야 한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAdminService {

  private final StaffRepository staffRepository;
  private final GenerationQueryService generationQueryService;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  /** 10.21 목록. */
  public List<StaffResult> getStaffs() {
    GenerationSummary activeGeneration = findActiveGeneration();
    List<Staff> staffs =
        staffRepository.findByGenerationNoOrderBySectionAscOrderAscIdAsc(activeGeneration.generationNo());

    Map<Long, String> profileImageUrls = findProfileImageUrls(staffs);

    return staffs.stream()
        .map(staff -> StaffResult.of(staff, profileImageUrls.get(staff.getFileId())))
        .toList();
  }

  /** 10.21 추가. order 는 해당 기수 · section 안에서 마지막 다음 순번으로 자동 부여한다. */
  @Transactional
  public StaffResult createStaff(StaffRequest request) {
    validateActiveGeneration(request.generationNo());
    if (request.fileId() != null) {
      validateAndConnectFile(request.fileId());
    }

    int nextOrder = nextOrderInSection(request.generationNo(), request.section());
    Staff saved = staffRepository.save(Staff.create(
        request.generationNo(), nextOrder, request.section(), request.staffRole(), request.name(),
        request.department(), request.introduction(), request.userId(), request.fileId()));

    return StaffResult.of(saved, resolveProfileImageUrl(request.fileId()));
  }

  /**
   * 10.21 수정. section 이 바뀌면 이전 section 에서 빠지는 자리를 당기고(삭제와 동일 원리)
   * 새 section 의 마지막 다음 순번으로 재배정한다.
   *
   * <p>파일 연결 갱신({@link #updateProfileFile})은 반드시 이 메서드의 마지막에 한다 —
   * {@code FileConnectionService} 내부의 원자적 UPDATE 가 {@code clearAutomatically = true}
   * 라 영속성 컨텍스트를 비우면서 이미 관리 중이던 {@code staff} 를 detach 시킨다. 그 전에
   * {@code staff.update(...)} 로 바꾼 내용은 {@code flushAutomatically = true} 덕분에 비워지기
   * 직전에 먼저 flush 되어 DB 에 반영되지만, 순서가 반대(파일 연결을 먼저)였다면 뒤이은
   * {@code staff.update(...)} 가 detach 된 엔티티에 적용돼 조용히 유실됐다(PR #82 Copilot
   * 리뷰 지적).
   */
  @Transactional
  public StaffResult updateStaff(Long staffId, StaffRequest request) {
    validateActiveGeneration(request.generationNo());
    Staff staff = findStaff(staffId, request.generationNo());
    Long previousFileId = staff.getFileId();

    if (staff.getSection() != request.section()) {
      reassignToNewSection(staff, request.generationNo(), request.section());
    }
    staff.update(
        request.section(), request.staffRole(), request.name(), request.department(),
        request.introduction(), request.userId(), request.fileId());

    updateProfileFile(previousFileId, request.fileId());

    return StaffResult.of(staff, resolveProfileImageUrl(request.fileId()));
  }

  /** 10.21 삭제. 삭제된 순번 뒤 운영진을 한 칸씩 당겨서 order 중복을 막는다({@code ApplicationQuestionService}와 동일 이유). */
  @Transactional
  public void deleteStaff(Long staffId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    Staff staff = findStaff(staffId, activeGeneration.generationNo());
    int deletedOrder = staff.getOrder();

    if (staff.getFileId() != null) {
      fileConnectionService.disconnectAll(List.of(staff.getFileId()));
    }
    staffRepository.delete(staff);

    staffRepository.findByGenerationNoAndSection(activeGeneration.generationNo(), staff.getSection()).stream()
        .filter(other -> other.getOrder() > deletedOrder)
        .forEach(other -> other.updateOrder(other.getOrder() - 1));
  }

  /**
   * 10.22. 배열 인덱스 순서대로 order 를 1부터 재부여한다. section 소속 운영진 전체가 중복 없이
   * 정확히 한 번씩 포함되어야 한다({@code ApplicationQuestionService.reorderQuestions}와 동일
   * 검증). 다만 이 위반들은 요청 역직렬화 검증이 아니라 도메인 규칙 위반이라 {@code
   * CommonErrorCode.VALIDATION_FAILED} 대신 전용 코드를 쓴다(PR #82 Copilot 리뷰 지적 —
   * {@code ApplicationQuestionService} 는 아직 이 지적 이전에 작성돼 그대로 남아있다).
   */
  @Transactional
  public void reorderStaffs(StaffSection section, List<Long> orderedIds) {
    if (new HashSet<>(orderedIds).size() != orderedIds.size()) {
      throw new BusinessException(StaffErrorCode.DUPLICATE_ORDER_ID);
    }

    GenerationSummary activeGeneration = findActiveGeneration();
    List<Staff> staffs = staffRepository.findByGenerationNoAndSection(activeGeneration.generationNo(), section);
    Map<Long, Staff> staffsById = staffs.stream().collect(Collectors.toMap(Staff::getId, Function.identity()));

    Set<Long> orderedIdSet = new HashSet<>(orderedIds);
    if (orderedIds.size() != staffs.size() || !staffsById.keySet().equals(orderedIdSet)) {
      throw new BusinessException(StaffErrorCode.INCOMPLETE_ORDER_SET);
    }

    for (int i = 0; i < orderedIds.size(); i++) {
      staffsById.get(orderedIds.get(i)).updateOrder(i + 1);
    }
  }

  private void reassignToNewSection(Staff staff, Integer generationNo, StaffSection newSection) {
    int previousOrder = staff.getOrder();
    StaffSection previousSection = staff.getSection();

    staffRepository.findByGenerationNoAndSection(generationNo, previousSection).stream()
        .filter(other -> !other.getId().equals(staff.getId()))
        .filter(other -> other.getOrder() > previousOrder)
        .forEach(other -> other.updateOrder(other.getOrder() - 1));

    staff.updateOrder(nextOrderInSection(generationNo, newSection));
  }

  /**
   * section 안의 마지막 다음 순번을 구한다. {@code findByGenerationNoAndSection} 이
   * {@code PESSIMISTIC_WRITE} 로 잠그므로, 동시에 들어온 생성 · section 이동 요청은 이 조회에서
   * 직렬화된다(PR #82 Copilot 리뷰 지적 — 이전에는 count 조회와 저장이 원자적이지 않아 두 요청이
   * 같은 순번을 계산할 수 있었다).
   */
  private int nextOrderInSection(Integer generationNo, StaffSection section) {
    return staffRepository.findByGenerationNoAndSection(generationNo, section).size() + 1;
  }

  /** 파일이 바뀌었을 때만 이전 파일 연결을 끊고 새 파일을 연결한다. */
  private void updateProfileFile(Long oldFileId, Long newFileId) {
    if (Objects.equals(oldFileId, newFileId)) {
      return;
    }
    if (oldFileId != null) {
      fileConnectionService.disconnectAll(List.of(oldFileId));
    }
    if (newFileId != null) {
      validateAndConnectFile(newFileId);
    }
  }

  /** 존재하지 않는 fileId 면 {@code FileQueryService} 가 던지는 예외를 그대로 전파한다. */
  private void validateAndConnectFile(Long fileId) {
    fileQueryService.findById(fileId);
    fileConnectionService.connectAll(List.of(fileId));
  }

  private String resolveProfileImageUrl(Long fileId) {
    return fileId == null ? null : fileQueryService.findById(fileId).url();
  }

  /**
   * 후보 전체의 fileId 를 모아 한 번에 조회한다 — 후보가 늘수록 단건 조회 쿼리가 급증하는 것을
   * 막는다. {@code Map.of()} (빈 불변 맵)는 null 키 조회에서 NPE 를 던지므로, fileId 가 없는
   * (null) 운영진이 있는 이 조회에는 쓸 수 없다 — {@link Collections#emptyMap()} 을 쓴다
   * ({@code UserAdminService.loadGroups} 와 동일한 이유).
   */
  private Map<Long, String> findProfileImageUrls(List<Staff> staffs) {
    List<Long> fileIds = staffs.stream().map(Staff::getFileId).filter(Objects::nonNull).distinct().toList();
    if (fileIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url));
  }

  private Staff findStaff(Long staffId, Integer generationNo) {
    return staffRepository.findByIdAndGenerationNo(staffId, generationNo)
        .orElseThrow(() -> new BusinessException(StaffErrorCode.STAFF_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(StaffErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  /** 요청받은 generationNo 가 활성 기수와 일치하는지 확인한다 ({@code UserPromotionService}와 동일 패턴). */
  private void validateActiveGeneration(Integer generationNo) {
    GenerationSummary activeGeneration = findActiveGeneration();
    if (!activeGeneration.generationNo().equals(generationNo)) {
      throw new BusinessException(StaffErrorCode.GENERATION_NOT_FOUND);
    }
  }
}
