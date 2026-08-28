package com.getit.domain.lecture.service;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LectureAccessServiceImpl implements LectureAccessService {

  private final GenerationQueryService generationQueryService;
  private final UserAccountService userAccountService;
  private final LectureRepository lectureRepository;

  @Override
  public void requireVisibleToMember(Long lectureId, Long userId) {
    GenerationSummary active = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    UserAccount me = userAccountService.findActiveById(userId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    if (me.role() != Role.MEMBER && me.role() != Role.ADMIN) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
    if (me.generationNo() == null || !me.generationNo().equals(active.generationNo())) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
    if (!lecture.isPublished() || !lecture.getGenerationId().equals(active.id())) {
      throw new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND);
    }
  }
}
