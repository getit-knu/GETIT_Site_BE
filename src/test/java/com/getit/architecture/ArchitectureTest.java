package com.getit.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

/**
 * 구조 규칙을 테스트로 강제한다.
 *
 * <p>도메인 경계는 리뷰로만 지키기 어렵다. 한 번 새면 되돌리는 비용이 크므로 CI 가 막는다.
 */
class ArchitectureTest {

  private static final String DOMAIN_PREFIX = "com.getit.domain.";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.getit");
  }

  /** com.getit.domain.&lt;이름&gt;... 에서 &lt;이름&gt; 을 꺼낸다. 하위 패키지가 깊어도 첫 조각을 쓴다. */
  private static String domainOf(JavaClass clazz) {
    String pkg = clazz.getPackageName();
    if (!pkg.startsWith(DOMAIN_PREFIX)) {
      return null;
    }
    String rest = pkg.substring(DOMAIN_PREFIX.length());
    int dot = rest.indexOf('.');
    return dot < 0 ? rest : rest.substring(0, dot);
  }

  /**
   * 다른 도메인이 건드리면 안 되는 내부 구현인가.
   *
   * <p>enum 은 뺀다. Role · UserStatus · EventType 같은 값 타입은 도메인을 넘어 쓰이는 것이
   * 정상이고, 이걸 막으면 의미 없는 래퍼만 늘어난다. 막아야 하는 것은 repository 접근과
   * 엔티티 자체를 들고 다니는 일이다.
   */
  private static boolean isInternal(JavaClass clazz) {
    String pkg = clazz.getPackageName();
    if (pkg.contains(".repository")) {
      return true;
    }
    return pkg.contains(".entity") && !clazz.isEnum();
  }

  @Test
  @DisplayName("다른 도메인의 repository · entity 를 직접 참조하지 않는다")
  void 도메인_간_내부_참조_금지() {
    List<String> violations = new ArrayList<>();

    for (JavaClass source : classes) {
      String from = domainOf(source);
      if (from == null) {
        continue;
      }
      for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
        JavaClass target = dependency.getTargetClass();
        String to = domainOf(target);
        if (to == null || from.equals(to) || !isInternal(target)) {
          continue;
        }
        violations.add(
            "%s -> %s".formatted(source.getName(), target.getName()));
      }
    }

    Assertions.assertThat(violations)
        .as("다른 도메인의 repository · entity 참조. <X>QueryService 계약을 통해야 한다")
        .isEmpty();
  }

  @Test
  @DisplayName("컨트롤러는 repository 를 직접 호출하지 않는다")
  void 컨트롤러는_repository_를_부르지_않는다() {
    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..repository..")
        .check(classes);
  }

  @Test
  @DisplayName("컨트롤러에 @Transactional 을 붙이지 않는다")
  void 컨트롤러에_트랜잭션_금지() {
    String transactional = "org.springframework.transaction.annotation.Transactional";

    noClasses()
        .that()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(transactional)
        .check(classes);

    noMethods()
        .that()
        .areDeclaredInClassesThat()
        .resideInAPackage("..controller..")
        .should()
        .beAnnotatedWith(transactional)
        .check(classes);
  }

  @Test
  @DisplayName("repository 는 service 를 참조하지 않는다")
  void repository_는_service_를_모른다() {
    noClasses()
        .that()
        .resideInAPackage("..repository..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..service..")
        .check(classes);
  }

  @Test
  @DisplayName("service 는 controller 를 참조하지 않는다")
  void service_는_controller_를_모른다() {
    noClasses()
        .that()
        .resideInAPackage("..service..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..controller..")
        .check(classes);
  }

  /**
   * global 은 공통 인프라라 특정 도메인의 구현을 몰라야 한다.
   *
   * <p>두 가지는 허용한다. 하나는 도메인의 enum — SecurityConfig 가 {@code Role.ADMIN} 으로
   * 경로 권한을 쓰는 것까지 막으면 같은 상수를 global 에 복제해야 한다. 다른 하나는 auth 도메인 —
   * SecurityConfig 는 JWT 필터와 핸들러를 엮어야 한다. 이걸 global 로 옮기면 인증 구현이 공통
   * 패키지로 새고, auth 로 옮기면 전 도메인의 경로 규칙이 한 도메인 안으로 들어간다.
   */
  @Test
  @DisplayName("global 은 도메인의 구현을 참조하지 않는다 (enum 과 auth 연결은 예외)")
  void global_은_도메인_구현을_모른다() {
    List<String> violations = new ArrayList<>();

    for (JavaClass source : classes) {
      if (!source.getPackageName().startsWith("com.getit.global")) {
        continue;
      }
      for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
        JavaClass target = dependency.getTargetClass();
        String domain = domainOf(target);
        if (domain == null) {
          continue;
        }
        boolean allowed = target.isEnum() || "auth".equals(domain);
        if (isInternal(target) || !allowed) {
          violations.add("%s -> %s".formatted(source.getName(), target.getName()));
        }
      }
    }

    Assertions.assertThat(violations)
        .as("global 이 도메인 구현에 의존한다")
        .isEmpty();
  }
}
