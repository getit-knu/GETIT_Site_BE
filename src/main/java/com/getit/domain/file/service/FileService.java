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
import com.getit.domain.file.storage.SignedUrl;
import com.getit.domain.file.storage.UploadTicket;
import com.getit.domain.user.entity.Role;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    String key = keyFor(purpose, extension);
    UploadTicket ticket = fileStorage.issueUploadTicket(key, request.contentType())
        .orElseThrow(() -> new BusinessException(FileErrorCode.DIRECT_UPLOAD_NOT_SUPPORTED));

    FileAsset saved = fileAssetRepository.save(FileAsset.upload(
        key, request.fileName(), ticket.fileUrl(), request.size(), request.contentType(), uploaderId));

    return PresignedUploadResponse.of(saved.getId(), ticket);
  }

  /**
   * 파일을 읽을 주소를 발급한다.
   *
   * <p><b>올린 본인과 ADMIN 만 쓸 수 있다.</b> 파일 도메인은 "이 사람이 이 강의를 듣는지",
   * "이 제출물이 본인 것인지" 를 알 수 없다. 연결된 파일이라고 열어주면 인증만 된 사람이
   * fileId 를 바꿔가며 남의 과제 제출물을 받을 수 있다(PR #126 Copilot 리뷰 지적).
   *
   * <p>연결된 파일은 소유 도메인이 권한을 확인한 뒤 {@code FileStorage.downloadUrl} 로
   * 직접 발급한다. 이 공용 경로는 그 우회로가 되지 않는다.
   */
  @Transactional(readOnly = true)
  public DownloadUrlResponse downloadUrl(Long fileId, Long requesterId, Role requesterRole) {
    FileAsset file = fileAssetRepository.findByIdAndDeletedAtIsNull(fileId)
        .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));

    boolean isOwner = file.getUploaderId().equals(requesterId);
    if (!isOwner && requesterRole != Role.ADMIN) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    SignedUrl signed = fileStorage.downloadUrl(file.getStoredKey());
    return DownloadUrlResponse.of(file, signed.url(), signed.expiresInSeconds());
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

  /**
   * 저장 키. 앞에 용도별 구분자를 붙인다.
   *
   * <p>{@code downloadUrl(key)} 은 용도를 모른 채 불린다. 키만 보고 공개·비공개
   * 저장소를 고를 수 있어야 해서 접두어로 드러낸다.
   */
  private String keyFor(FilePurpose purpose, String extension) {
    return purpose.keyPrefix() + "/" + UUID.randomUUID() + "." + extension;
  }

  private String extensionOf(String originalName) {
    String extension = StringUtils.getFilenameExtension(originalName);
    if (!StringUtils.hasText(extension)) {
      throw new BusinessException(FileErrorCode.INVALID_FILE_EXTENSION);
    }
    return extension;
  }
}
