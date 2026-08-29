package com.getit.global.config;

import java.util.Set;

import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.core.jackson.TypeNameResolver;

/**
 * 중첩 타입의 OpenAPI 스키마 이름에 바깥 클래스 이름을 붙인다.
 *
 * <p>기본 동작은 중첩 클래스를 <b>단순 이름</b>으로만 등록한다. 그래서 여러 도메인이
 * {@code Create} · {@code Detail} · {@code ListResult} 같은 흔한 이름을 각자 쓰면
 * 스키마 하나에 모두 몰리고, <b>마지막에 등록된 것이 앞의 것을 덮어쓴다.</b>
 *
 * <p>실제로 {@code POST /api/admin/lectures} 의 요청 스키마가
 * {@code LectureRequest.Create} 가 아니라 다른 도메인의 {@code Create} 로 나갔다.
 * 프론트는 이 문서로 타입을 생성하므로, 문서가 틀리면 실제와 다른 모양을 그대로 믿는다
 * (이슈 #137, FE 저장소 #192 작업 중 발견).
 *
 * <p>DTO 마다 {@code @Schema(name = ...)} 를 다는 방법도 있지만 37개를 고쳐야 하고,
 * 새로 만드는 DTO 에서 같은 문제가 다시 생긴다. 이름 짓는 규칙 자체를 바꿔서
 * 앞으로 만들어지는 것까지 자동으로 안전하게 한다.
 *
 * <pre>
 *   LectureRequest.Create      → LectureRequestCreate
 *   LectureAdminResult.ListResult → LectureAdminResultListResult
 * </pre>
 *
 * <p>점이 아니라 이어 붙인다. 스키마 이름의 점은 코드 생성기가 식별자로 바꾸면서
 * 제각각 망가뜨린다.
 */
public class NestedAwareTypeNameResolver extends TypeNameResolver {

  @Override
  protected String nameForClass(Class<?> cls, Set<Options> options) {
    Class<?> enclosing = cls.getEnclosingClass();
    if (enclosing == null || PrimitiveType.fromType(cls) != null) {
      return super.nameForClass(cls, options);
    }
    // 3단 이상 중첩도 바깥에서 안쪽 순서로 모두 붙인다.
    return nameForClass(enclosing, options) + super.nameForClass(cls, options);
  }
}
