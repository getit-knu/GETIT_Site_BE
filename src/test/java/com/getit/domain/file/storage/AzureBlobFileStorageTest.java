package com.getit.domain.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;

/**
 * SAS 발급 동작을 고정한다.
 *
 * <p>실제 Azure 를 부르지 않는다. 클라이언트와 {@link Clock} 을 밖에서 넣을 수 있게
 * 만들어 둔 덕분에 권한·헤더·만료·위임 키 재사용을 결정적으로 확인할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AzureBlobFileStorageTest {

  private static final Instant FIXED = Instant.parse("2026-08-29T00:00:00Z");
  private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
  private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);
  private static final Duration KEY_TTL = Duration.ofHours(6);

  @Mock
  private BlobServiceClient serviceClient;

  @Mock
  private BlobContainerClient container;

  @Mock
  private BlobClient blob;

  @Mock
  private UserDelegationKey delegationKey;

  private AzureBlobFileStorage storage;

  @BeforeEach
  void setUp() {
    given(serviceClient.getBlobContainerClient(anyString())).willReturn(container);
    given(container.getBlobClient(anyString())).willReturn(blob);
    given(blob.getBlobUrl()).willReturn("https://acct.blob.core.windows.net/uploads/k.pdf");
    given(serviceClient.getUserDelegationKey(any(), any())).willReturn(delegationKey);
    given(blob.generateUserDelegationSas(any(), any())).willReturn("sv=2021&sig=abc");

    storage = new AzureBlobFileStorage(
        serviceClient, "uploads",
        Clock.fixed(FIXED, ZoneOffset.UTC),
        UPLOAD_TTL, DOWNLOAD_TTL, KEY_TTL);
  }

  private BlobServiceSasSignatureValues captureSignatureValues() {
    ArgumentCaptor<BlobServiceSasSignatureValues> captor =
        ArgumentCaptor.forClass(BlobServiceSasSignatureValues.class);
    verify(blob).generateUserDelegationSas(captor.capture(), any());
    return captor.getValue();
  }

  @Test
  @DisplayName("업로드 티켓은 create 권한만 준다")
  void uploadTicketGrantsCreateOnly() {
    storage.issueUploadTicket("k.pdf", "application/pdf");

    BlobServiceSasSignatureValues values = captureSignatureValues();
    assertThat(values.getPermissions()).contains("c");
    // write 까지 주면 만료 전까지 같은 주소로 덮어쓸 수 있어, 연결 직전 검사를 통과한 뒤
    // 다른 파일로 바꿔치기할 수 있다.
    assertThat(values.getPermissions()).doesNotContain("w");
  }

  @Test
  @DisplayName("업로드 티켓에 블록 blob 헤더와 만료 시간을 담는다")
  void uploadTicketCarriesRequiredHeaders() {
    UploadTicket ticket = storage.issueUploadTicket("k.pdf", "application/pdf").orElseThrow();

    assertThat(ticket.method()).isEqualTo("PUT");
    // 이 헤더가 빠지면 저장소가 400 으로 거부한다.
    assertThat(ticket.headers()).containsEntry("x-ms-blob-type", "BlockBlob");
    assertThat(ticket.headers()).containsEntry("Content-Type", "application/pdf");
    assertThat(ticket.expiresInSeconds()).isEqualTo((int) UPLOAD_TTL.toSeconds());
    assertThat(ticket.uploadUrl()).contains("sig=");
    // 고정 주소에는 서명이 붙지 않는다. 연결 후 저장되는 값이다.
    assertThat(ticket.fileUrl()).doesNotContain("sig=");
  }

  @Test
  @DisplayName("다운로드 주소는 읽기 권한만 주고 만료를 함께 알린다")
  void downloadUrlGrantsReadOnlyAndReportsExpiry() {
    SignedUrl signed = storage.downloadUrl("k.pdf");

    BlobServiceSasSignatureValues values = captureSignatureValues();
    assertThat(values.getPermissions()).contains("r");
    assertThat(values.getPermissions()).doesNotContain("w").doesNotContain("c");
    assertThat(signed.expiresInSeconds()).isEqualTo((int) DOWNLOAD_TTL.toSeconds());
    assertThat(signed.url()).contains("sig=");
  }

  @Test
  @DisplayName("서명 만료는 주입된 Clock 기준으로 정해진다")
  void expiryFollowsInjectedClock() {
    storage.downloadUrl("k.pdf");

    assertThat(captureSignatureValues().getExpiryTime().toInstant())
        .isEqualTo(FIXED.plus(DOWNLOAD_TTL));
  }

  @Test
  @DisplayName("위임 키는 한 번만 받아 재사용한다")
  void reusesDelegationKey() {
    storage.issueUploadTicket("a.pdf", "application/pdf");
    storage.issueUploadTicket("b.pdf", "application/pdf");
    storage.downloadUrl("c.pdf");

    // 발급마다 Azure AD 를 부르면 업로드 요청마다 왕복이 하나씩 붙는다.
    verify(serviceClient, times(1)).getUserDelegationKey(any(), any());
  }
}
