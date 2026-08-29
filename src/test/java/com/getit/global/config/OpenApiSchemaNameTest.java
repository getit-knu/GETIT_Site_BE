package com.getit.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OpenAPI 문서의 스키마 이름이 도메인 간에 겹치지 않는지 본다.
 *
 * <p>겹치면 마지막에 등록된 스키마가 앞의 것을 덮어써서, 문서가 <b>실제와 다른 모양</b>을
 * 내보낸다. 프론트는 이 문서로 타입을 생성하므로 조용히 잘못된 타입을 믿게 된다.
 * 실제로 강의 등록 요청이 다른 도메인의 {@code Create} 로 나갔다 (이슈 #137).
 *
 * <p>사람 눈에는 안 보이는 종류의 고장이라 테스트로 못을 박는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiSchemaNameTest {

  private static JsonNode apiDocs;

  @Autowired
  private MockMvc mockMvc;

  @BeforeAll
  static void reset() {
    apiDocs = null;
  }

  private JsonNode docs() throws Exception {
    if (apiDocs == null) {
      String json = mockMvc.perform(get("/v3/api-docs"))
          .andReturn().getResponse().getContentAsString();
      apiDocs = new ObjectMapper().readTree(json);
    }
    return apiDocs;
  }

  private JsonNode schemaOf(String ref) throws Exception {
    // "#/components/schemas/Xxx" 에서 이름만 떼어낸다
    return docs().at("/components/schemas/" + ref.substring(ref.lastIndexOf('/') + 1));
  }

  /** 응답 content 의 미디어 타입 키가 구현마다 달라서(`*​/*` 등) 첫 항목을 따라간다. */
  private String refUnder(JsonNode contentHolder) {
    JsonNode content = contentHolder.get("content");
    JsonNode first = content.elements().next();
    return first.at("/schema/$ref").asText();
  }

  @Test
  @DisplayName("강의 등록 요청 스키마가 실제 LectureRequest.Create 를 가리킨다")
  void lectureCreateRequestPointsToRealSchema() throws Exception {
    String ref = refUnder(docs().at("/paths/~1api~1admin~1lectures/post/requestBody"));

    assertThat(ref).endsWith("LectureRequestCreate");
    JsonNode properties = schemaOf(ref).get("properties");

    // 예전에는 다른 도메인의 {content} 하나짜리 Create 를 가리켰다.
    assertThat(properties.fieldNames()).toIterable()
        .contains("generationId", "trackId", "subCategoryId", "week", "title", "fileIds");
  }

  @Test
  @DisplayName("강의 목록 응답 스키마가 관리자용 ListResult 를 가리킨다")
  void lectureListResponsePointsToAdminSchema() throws Exception {
    String ref = refUnder(docs().at("/paths/~1api~1admin~1lectures/get/responses/200"));

    // 회원용 LectureResult.ListResult 가 아니라 관리자용이어야 한다.
    // 예전에는 둘 다 그냥 ListResult 였고 나중 것이 앞의 것을 덮어썼다.
    assertThat(ref).endsWith("LectureAdminResultListResult");

    JsonNode data = schemaOf(ref).at("/properties/data");
    JsonNode target = data.has("$ref") ? schemaOf(data.get("$ref").asText()) : data;
    assertThat(target.get("properties").fieldNames()).toIterable()
        .contains("tracks", "lectures");
  }

  @Test
  @DisplayName("중첩 타입은 바깥 클래스 이름을 달고 등록된다")
  void nestedTypesCarryOuterName() throws Exception {
    List<String> names = new ArrayList<>();
    docs().at("/components/schemas").fieldNames().forEachRemaining(names::add);

    // 이 이름들이 그대로 있으면 도메인 구분 없이 등록됐다는 뜻이다.
    assertThat(names)
        .doesNotContain("Create", "Detail", "Write", "ListResult", "CreateResult", "UpdateResult");
    assertThat(names).contains("LectureRequestCreate");
  }
}
