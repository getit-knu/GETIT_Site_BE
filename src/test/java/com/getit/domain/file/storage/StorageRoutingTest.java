package com.getit.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.getit.domain.file.entity.FilePurpose;

/**
 * 저장 키 접두어로 공개·비공개 저장소가 갈리는지 본다.
 *
 * <p>{@code downloadUrl(key)} 은 용도를 모른 채 불린다. 접두어가 유일한 단서라
 * 여기가 틀리면 강의 자료가 공개 저장소로 가거나 그 반대가 된다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StorageRoutingTest {

  @Mock
  private FileStorage publicStorage;

  @Mock
  private FileStorage privateStorage;

  private RoutingFileStorage routing() {
    return new RoutingFileStorage(publicStorage, privateStorage);
  }

  @Test
  @DisplayName("public/ 로 시작하는 키는 공개 저장소로 간다")
  void routesPublicKeys() {
    given(publicStorage.downloadUrl(anyString())).willReturn(SignedUrl.permanent("https://p/x.png"));

    routing().downloadUrl("public/x.png");

    verify(publicStorage).downloadUrl("public/x.png");
    verify(privateStorage, never()).downloadUrl(anyString());
  }

  @Test
  @DisplayName("private/ 로 시작하는 키는 비공개 저장소로 간다")
  void routesPrivateKeys() {
    given(privateStorage.downloadUrl(anyString())).willReturn(new SignedUrl("https://q/x.pdf?sig=1", 300));

    routing().downloadUrl("private/x.pdf");

    verify(privateStorage).downloadUrl("private/x.pdf");
    verify(publicStorage, never()).downloadUrl(anyString());
  }

  @Test
  @DisplayName("접두어가 없는 옛 키는 비공개로 보낸다")
  void unknownPrefixFallsBackToPrivate() {
    given(privateStorage.describe(anyString())).willReturn(java.util.Optional.empty());

    routing().describe("legacy-uuid.pdf");

    // 판단이 서지 않을 때 공개로 보내면 비공개여야 할 파일이 열린다.
    verify(privateStorage).describe("legacy-uuid.pdf");
    verify(publicStorage, never()).describe(anyString());
  }

  @Test
  @DisplayName("용도별 접두어가 의도한 저장소를 가리킨다")
  void purposePrefixesMatchVisibility() {
    assertThat(FilePurpose.LECTURE_MATERIAL.keyPrefix()).isEqualTo("private");
    assertThat(FilePurpose.ASSIGNMENT.keyPrefix()).isEqualTo("private");
    assertThat(FilePurpose.PROFILE_IMAGE.keyPrefix()).isEqualTo("public");
    assertThat(FilePurpose.PROJECT_THUMBNAIL.keyPrefix()).isEqualTo("public");
  }

  @Nested
  @DisplayName("공개 저장소의 읽기 주소")
  class PublicReads {

    @Mock
    private BlobServiceClient serviceClient;

    @Mock
    private BlobContainerClient container;

    @Mock
    private BlobClient blob;

    @Mock
    private UserDelegationKey delegationKey;

    private AzureBlobFileStorage publicAzure() {
      given(serviceClient.getBlobContainerClient(anyString())).willReturn(container);
      given(container.getBlobClient(anyString())).willReturn(blob);
      given(blob.getBlobUrl()).willReturn("https://getitpublic01.blob.core.windows.net/public-assets/p.png");
      given(serviceClient.getUserDelegationKey(any(), any())).willReturn(delegationKey);
      given(blob.generateUserDelegationSas(any(), any())).willReturn("sv=2021&sig=abc");
      return new AzureBlobFileStorage(
          serviceClient, "public-assets", true,
          Clock.fixed(Instant.parse("2026-08-29T00:00:00Z"), ZoneOffset.UTC),
          Duration.ofMinutes(10), Duration.ofMinutes(5), Duration.ofHours(6));
    }

    @Test
    @DisplayName("서명을 붙이지 않는다 — 붙이면 5분마다 URL 이 바뀌어 캐시가 안 걸린다")
    void publicDownloadUrlIsNotSigned() {
      SignedUrl signed = publicAzure().downloadUrl("public/p.png");

      assertThat(signed.url()).doesNotContain("sig=");
      assertThat(signed.expiresInSeconds()).isEqualTo(SignedUrl.NEVER_EXPIRES);
    }

    @Test
    @DisplayName("공개 저장소여도 업로드는 서명이 필요하다")
    void publicUploadStillRequiresSignature() {
      UploadTicket ticket = publicAzure().issueUploadTicket("public/p.png", "image/png").orElseThrow();

      // 읽기는 열려 있어도 아무나 쓰게 두면 안 된다.
      assertThat(ticket.uploadUrl()).contains("sig=");
    }
  }
}
