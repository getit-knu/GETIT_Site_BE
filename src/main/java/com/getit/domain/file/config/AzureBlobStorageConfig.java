package com.getit.domain.file.config;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.getit.domain.file.storage.AzureBlobFileStorage;

/**
 * Azure Blob 저장소 조립.
 *
 * <p>SDK 클라이언트와 {@link Clock} 을 밖에서 만들어 넣는다. 저장소 구현이 직접
 * 생성하면 단위 테스트에서 실제 Azure 호출 없이 서명·만료 동작을 확인할 수 없다.
 */
@Configuration
@ConditionalOnProperty(prefix = "getit.file.azure", name = "enabled", havingValue = "true")
public class AzureBlobStorageConfig {

  /**
   * 관리 ID 로 인증한다. 계정 키를 설정에 두지 않는다.
   *
   * <p>{@code DefaultAzureCredential} 은 VM 에서는 관리 ID 를, 개발자 PC 에서는
   * {@code az login} 세션을 쓴다.
   */
  @Bean
  public BlobServiceClient blobServiceClient(
      @Value("${getit.file.azure.account}") String account
  ) {
    return new BlobServiceClientBuilder()
        .endpoint("https://%s.blob.core.windows.net".formatted(account))
        .credential(new DefaultAzureCredentialBuilder().build())
        .buildClient();
  }

  @Bean
  public AzureBlobFileStorage azureBlobFileStorage(
      BlobServiceClient blobServiceClient,
      @Value("${getit.file.azure.container}") String container,
      @Value("${getit.file.azure.upload-url-ttl}") Duration uploadTtl,
      @Value("${getit.file.azure.download-url-ttl}") Duration downloadTtl,
      @Value("${getit.file.azure.delegation-key-ttl}") Duration delegationKeyTtl
  ) {
    return new AzureBlobFileStorage(
        blobServiceClient,
        container,
        // Clock 을 빈으로 올리지 않는다. 이 코드베이스에는 이미 recruitmentClock 이 있어
        // 타입으로 주입받는 기존 서비스가 후보 둘 사이에서 해석되지 않는다.
        Clock.system(ZoneId.of("Asia/Seoul")),
        uploadTtl,
        downloadTtl,
        delegationKeyTtl);
  }
}
