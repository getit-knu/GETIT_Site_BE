package com.getit.global.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 사용자가 입력해 화면의 링크로 쓰이는 주소.
 *
 * <p><b>스킴을 http · https 로 제한한다.</b> 화면이 이 값을 그대로 {@code href} 에 넣기 때문에,
 * {@code javascript:} 같은 스킴을 허용하면 그 페이지가 XSS · 피싱 통로가 된다.
 * 어드민만 입력할 수 있어도 계정 하나가 털리면 공개 페이지 전체가 열린다 (이슈 #159).
 *
 * <p>길이는 컬럼에 맞춘 512 자다.
 *
 * <p>{@code null} 과 빈 문자열은 통과한다 — 이런 주소는 대체로 선택값이고, 필수 여부는
 * 쓰는 쪽에서 {@code @NotBlank} 로 따로 정한다.
 *
 * <p>빈 문자열을 정규식에 넣어 둔 이유가 있다. {@code @Pattern} 은 {@code null} 은 검사하지
 * 않지만 빈 문자열은 검사한다. 화면의 입력칸을 비워 두면 브라우저는 {@code null} 이 아니라
 * {@code ""} 를 보내므로, 선택값인데도 400 이 났다 (이슈 #176).
 *
 * <p>규칙을 한 곳에 둔다. 필드마다 정규식을 적어두면 새 필드가 생길 때 빠뜨리게 되고,
 * 실제로 운영진 링크만 막고 프로젝트 · 강의 링크가 열려 있는 상태가 한동안 있었다.
 */
@Target({FIELD, PARAMETER, RECORD_COMPONENT, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Size(max = 512)
@Pattern(regexp = "^$|^https?://\\S+$")
@Documented
public @interface HttpUrl {

  String message() default "http 또는 https 로 시작하는 주소여야 합니다.";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
