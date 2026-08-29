package com.getit.domain.file.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import com.azure.core.exception.HttpResponseException;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.common.sas.SasProtocol;

/**
 * Azure Blob Storage 구현.
 *
 * <p>업로드는 클라이언트가 저장소로 직접 보낸다. 서버는 서명된 주소만 발급한다.
 *
 * <h2>계정 키를 두지 않는다</h2>
 *
 * <p>인증은 VM 의 관리 ID 로 한다. 서명도 계정 키가 아니라 <b>사용자 위임 키</b>로 하므로,
 * 설정 어디에도 장기 자격증명이 없다. 키를 두면 설정 파일 하나가 새는 것만으로 저장소
 * 전체가 열린다.
 *
 * <p>컨테이너는 비공개다. 그래서 읽을 때도 매번 짧게 사는 서명 주소를 발급한다.
 *
 * <p>시각은 {@link Clock} 을 주입받는다. 만료·갱신 경계를 시스템 시각에 직접 묶으면
 * 테스트가 실행 시점에 따라 달라진다(PR #126 Copilot 리뷰 지적).
 */
@Slf4j
public class AzureBlobFileStorage implements FileStorage {

  /** 브라우저가 블록 blob 업로드에 반드시 실어야 하는 헤더. 빠지면 400 이 난다. */
  private static final String BLOB_TYPE_HEADER = "x-ms-blob-type";
  private static final String BLOCK_BLOB = "BlockBlob";

  /** 위임 키를 만료 직전까지 쓰지 않고 미리 갱신한다. 경계에서 실패하지 않게 한다. */
  private static final Duration KEY_RENEW_MARGIN = Duration.ofMinutes(30);

  /** 서버 시계가 Azure 보다 빠르면 방금 만든 키가 즉시 만료로 거부된다. */
  private static final Duration CLOCK_SKEW = Duration.ofMinutes(5);

  private final BlobServiceClient serviceClient;
  private final BlobContainerClient container;
  private final Clock clock;

  /** 공개 컨테이너면 읽기에 서명이 필요 없다. 고정 주소라 브라우저 캐시가 걸린다. */
  private final boolean publiclyReadable;
  private final Duration uploadTtl;
  private final Duration downloadTtl;
  private final Duration delegationKeyTtl;

  private volatile UserDelegationKey delegationKey;
  private volatile OffsetDateTime delegationKeyRenewAt;

  public AzureBlobFileStorage(
      BlobServiceClient serviceClient,
      String containerName,
      boolean publiclyReadable,
      Clock clock,
      Duration uploadTtl,
      Duration downloadTtl,
      Duration delegationKeyTtl
  ) {
    this.serviceClient = serviceClient;
    this.container = serviceClient.getBlobContainerClient(containerName);
    this.publiclyReadable = publiclyReadable;
    this.clock = clock;
    this.uploadTtl = uploadTtl;
    this.downloadTtl = downloadTtl;
    this.delegationKeyTtl = delegationKeyTtl;
  }

  /**
   * 서버를 거치는 업로드. (명세 13.2)
   *
   * <p>직접 업로드가 기본이지만, 서버가 만든 파일이나 작은 파일에는 이 경로가 편하다.
   */
  @Override
  public String upload(MultipartFile file, String key) {
    BlobClient blob = container.getBlobClient(key);
    try (InputStream in = file.getInputStream()) {
      blob.upload(in, file.getSize(), true);
    } catch (IOException e) {
      throw new UncheckedIOException("파일 업로드 실패: key=" + key, e);
    }
    blob.setHttpHeaders(new BlobHttpHeaders().setContentType(file.getContentType()));
    return blob.getBlobUrl();
  }

  @Override
  public void delete(String key) {
    // 컨테이너에 soft delete 가 걸려 있어 30 일 안에는 되돌릴 수 있다.
    container.getBlobClient(key).deleteIfExists();
  }

  @Override
  public Optional<UploadTicket> issueUploadTicket(String key, String contentType) {
    BlobClient blob = container.getBlobClient(key);

    // create 만 준다. write 까지 주면 만료 전까지 같은 주소로 몇 번이든 덮어쓸 수 있어,
    // 연결 직전 검사를 통과한 뒤 다른 파일로 바꿔치기할 수 있다.
    String sas = sign(blob, new BlobSasPermission().setCreatePermission(true), uploadTtl);

    return Optional.of(new UploadTicket(
        blob.getBlobUrl() + "?" + sas,
        blob.getBlobUrl(),
        "PUT",
        Map.of(BLOB_TYPE_HEADER, BLOCK_BLOB, "Content-Type", contentType),
        (int) uploadTtl.toSeconds()));
  }

  @Override
  public SignedUrl downloadUrl(String key) {
    BlobClient blob = container.getBlobClient(key);
    if (publiclyReadable) {
      // 서명을 붙이면 5분마다 URL 이 바뀌어 캐시가 걸리지 않는다.
      return SignedUrl.permanent(blob.getBlobUrl());
    }
    String sas = sign(blob, new BlobSasPermission().setReadPermission(true), downloadTtl);
    return new SignedUrl(blob.getBlobUrl() + "?" + sas, (int) downloadTtl.toSeconds());
  }

  @Override
  public Optional<StoredObject> describe(String key) {
    try {
      BlobProperties properties = container.getBlobClient(key).getProperties();
      return Optional.of(new StoredObject(properties.getBlobSize(), properties.getContentType()));
    } catch (HttpResponseException e) {
      // 아직 올리지 않았거나 이미 지워졌다. 호출한 쪽이 "없음" 으로 처리한다.
      log.debug("blob 정보를 읽지 못했습니다. key={} status={}", key, e.getResponse().getStatusCode());
      return Optional.empty();
    }
  }

  private String sign(BlobClient blob, BlobSasPermission permission, Duration ttl) {
    OffsetDateTime expiry = now().plus(ttl);
    BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiry, permission)
        // http 로 전달되면 서명이 그대로 노출된다.
        .setProtocol(SasProtocol.HTTPS_ONLY);
    return blob.generateUserDelegationSas(values, delegationKey());
  }

  /**
   * 사용자 위임 키를 받아 재사용한다.
   *
   * <p>발급마다 Azure AD 를 호출하면 업로드 요청마다 왕복이 하나 더 붙는다.
   * 키는 며칠 동안 유효하므로 만료 전까지 들고 쓴다.
   */
  private UserDelegationKey delegationKey() {
    OffsetDateTime now = now();
    UserDelegationKey cached = delegationKey;
    if (cached != null && delegationKeyRenewAt != null && now.isBefore(delegationKeyRenewAt)) {
      return cached;
    }
    synchronized (this) {
      if (delegationKey == null || delegationKeyRenewAt == null
          || !now.isBefore(delegationKeyRenewAt)) {
        OffsetDateTime expiry = now.plus(delegationKeyTtl);
        delegationKey = serviceClient.getUserDelegationKey(now.minus(CLOCK_SKEW), expiry);
        delegationKeyRenewAt = expiry.minus(KEY_RENEW_MARGIN);
      }
      return delegationKey;
    }
  }

  private OffsetDateTime now() {
    return OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
  }
}
