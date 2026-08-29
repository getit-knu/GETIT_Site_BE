package com.getit.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: /swagger-ui.html · OpenAPI 문서: /v3/api-docs
 * 본 문서는 DOCS/API_명세서.pdf 와 동기화 상태를 유지한다.
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_SCHEME = "bearerAuth";

  /**
   * 스키마 이름 규칙을 바꾼다. 중첩 타입에 바깥 클래스 이름을 붙여 도메인 간 충돌을 없앤다.
   *
   * <p>springdoc 은 {@code ModelResolver} 를 직접 만들되 이미 등록된 빈이 있으면
   * 그것을 쓴다. 여기서 갈아끼우면 문서 전체에 규칙이 적용된다.
   */
  @Bean
  public ModelResolver modelResolver(ObjectMapper objectMapper) {
    return new ModelResolver(objectMapper, new NestedAwareTypeNameResolver());
  }

  @Bean
  public OpenAPI openAPI() {
    SecurityScheme securityScheme = new SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("bearer")
        .bearerFormat("JWT")
        .in(SecurityScheme.In.HEADER)
        .name("Authorization");

    return new OpenAPI()
        .info(new Info()
            .title("GETIT API")
            .description("GETIT 동아리 통합 사이트 백엔드 API")
            .version("v1"))
        .components(new Components().addSecuritySchemes(BEARER_SCHEME, securityScheme))
        .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
  }
}
