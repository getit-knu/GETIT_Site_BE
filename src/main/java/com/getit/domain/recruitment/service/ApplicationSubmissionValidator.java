package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 지원서 제출(3.4) 검증 4~6단계. (API 명세서 3.4)
 *
 * <p>PR #46 리뷰 대응(도메인 ErrorCode 적용 등)으로 {@link ApplicationService} 가 300줄 제한을
 * 넘겨 검증 로직만 분리했다. 상태를 갖지 않는 순수 검증이라 Bean 으로 등록하지 않고 패키지 내부
 * 전용 static 메서드로 둔다.
 */
final class ApplicationSubmissionValidator {

  private ApplicationSubmissionValidator() { }

  /** 4단계: 기본 정보(이름 · 이메일 · 연락처 · 단과대학 · 전공 · 학년) 필수. */
  static void validateBasicInfo(Application application) {
    boolean missing = !StringUtils.hasText(application.getName())
        || !StringUtils.hasText(application.getEmail())
        || !StringUtils.hasText(application.getPhoneNumber())
        || application.getCollegeId() == null
        || application.getMajorId() == null
        || application.getGrade() == null;
    if (missing) {
      throw new BusinessException(RecruitmentErrorCode.BASIC_INFO_INCOMPLETE);
    }
  }

  /** 5단계: required = true 질문은 전부 응답해야 한다. */
  static void validateRequiredAnswers(List<ApplicationQuestion> questions, List<ApplicationAnswer> answers) {
    Map<Long, ApplicationAnswer> answersByQuestionId = answers.stream()
        .collect(Collectors.toMap(ApplicationAnswer::getQuestionId, Function.identity()));

    for (ApplicationQuestion question : questions) {
      if (!question.isRequired()) {
        continue;
      }
      ApplicationAnswer answer = answersByQuestionId.get(question.getId());
      if (!isAnswered(answer)) {
        throw new BusinessException(RecruitmentErrorCode.REQUIRED_ANSWER_MISSING);
      }
    }
  }

  private static boolean isAnswered(ApplicationAnswer answer) {
    if (answer == null) {
      return false;
    }
    return StringUtils.hasText(answer.getAnswerText())
        || (answer.getSelectedOptions() != null && !answer.getSelectedOptions().isEmpty());
  }

  /** 6단계: 각 답변은 질문의 maxLength 이내여야 한다. */
  static void validateAnswerLengths(List<ApplicationQuestion> questions, List<ApplicationAnswer> answers) {
    Map<Long, ApplicationQuestion> questionsById = questions.stream()
        .collect(Collectors.toMap(ApplicationQuestion::getId, Function.identity()));

    for (ApplicationAnswer answer : answers) {
      ApplicationQuestion question = questionsById.get(answer.getQuestionId());
      if (question == null || question.getMaxLength() == null || answer.getAnswerText() == null) {
        continue;
      }
      if (answer.getAnswerText().length() > question.getMaxLength()) {
        throw new BusinessException(RecruitmentErrorCode.ANSWER_LENGTH_EXCEEDED);
      }
    }
  }
}
