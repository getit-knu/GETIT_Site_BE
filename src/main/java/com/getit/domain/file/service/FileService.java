package com.getit.domain.file.service;

import com.getit.domain.file.dto.DownloadUrlResponse;
import com.getit.domain.file.dto.FileUploadResponse;
import com.getit.domain.file.dto.PresignedUploadRequest;
import com.getit.domain.file.dto.PresignedUploadResponse;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FilePurpose;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.UploadTicket;
import com.getit.domain.user.entity.Role;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
@RequiredArgsConstructor
public class FileService {

  private final FileStorage fileStorage;
  private final FileAssetRepository fileAssetRepository;

  /** 응답에 적어 내려주는 값. 실제 만료는 저장소가 서명할 때 정한다. */
  @Value("${getit.file.azure.download-url-ttl}")
  private Duration downloadUrlTtl;

  public FileUploadResponse upload(MultipartFile file, FilePurpose purpose, Long uploaderId) {
    String extension = extensionOf(file.getOriginalFilename());
    if (!purpose.allows(extension)) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_EXTENSION);
    }
    if (file.getSize() > purpose.getMaxSizeBytes()) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_SIZE);
    }

    String key = UUID.randomUUID() + "." + extension;
    String url = fileStorage.upload(file, key);

    FileAsset saved = fileAssetRepository.save(
        FileAsset.upload(key, file.getOriginalFilename(), url, file.getSize(), file.getContentType(), uploaderId)
    );

    return FileUploadResponse.from(saved);
  }

  /**
   * 클라이언트가 저장소로 직접 올릴 주소를 발급한다. (명세 13.1)
   *
   * <p>발급 시점에 파일 레코드를 PENDING 으로 만들어 둔다. 클라이언트가 업로드를 끝낸 뒤
   * 도메인 API 에 이 {@code fileId} 를 넘겨 연결한다. 연결되지 않은 레코드는 정리 배치가 지운다.
   *
   * <p>검증은 발급 전에 한다. 올린 뒤에 거절하면 이미 저장소에 파일이 들어가 있다.
   */
  public PresignedUploadResponse issueUploadUrl(PresignedUploadRequest request, Long uploaderId) {
    String extension = extensionOf(request.fileName());
    FilePurpose purpose = request.purpose();
    if (!purpose.allows(extension)) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_EXTENSION);
    }
    if (request.size() > purpose.getMaxSizeBytes()) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_SIZE);
    }

    String key = UUID.randomUUID() + "." + extension;
    UploadTicket ticket = fileStorage.issueUploadTicket(key, request.contentType())
        .orElseThrow(() -> new BusinessException(FileErrorCode.DIRECT_UPLOAD_NOT_SUPPORTED));

    FileAsset saved = fileAssetRepository.save(FileAsset.upload(
        key, request.fileName(), ticket.fileUrl(), request.size(), request.contentType(), uploaderId));

    return PresignedUploadResponse.of(saved.getId(), ticket);
  }

  /**
   * 파일을 읽을 주소를 발급한다. (명세 4.3)
   *
   * <p>아직 리소스에 연결되지 않은 파일은 올린 본인과 ADMIN 만 볼 수 있다.
   * 연결된 파일의 세부 권한(수강 여부 등)은 파일 도메인이 알 수 없으므로
   * 각 도메인에서 확인한 뒤 이 메서드를 부른다.
   */
  @Transactional(readOnly = true)
  public DownloadUrlResponse downloadUrl(Long fileId, Long requesterId, Role requesterRole) {
    FileAsset file = fileAssetRepository.findByIdAndDeletedAtIsNull(fileId)
        .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

    boolean isOwner = file.getUploaderId().equals(requesterId);
    if (!file.isInUse() && !isOwner && requesterRole != Role.ADMIN) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    return DownloadUrlResponse.of(
        file, fileStorage.downloadUrl(file.getStoredKey()), (int) downloadUrlTtl.toSeconds());
  }

  public void delete(Long fileId, Long requesterId, Role requesterRole) {
    FileAsset file = fileAssetRepository.findByIdAndDeletedAtIsNull(fileId)
        .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

    boolean isOwner = file.getUploaderId().equals(requesterId);
    if (!isOwner && requesterRole != Role.ADMIN) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }
    if (file.isInUse()) {
      throw new BusinessException(FileErrorCode.FILE_IN_USE);
    }

    fileStorage.delete(file.getStoredKey());
    file.delete();
  }

  private String extensionOf(String originalName) {
    String extension = StringUtils.getFilenameExtension(originalName);
    if (!StringUtils.hasText(extension)) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_EXTENSION);
    }
    return extension;
  }
}
