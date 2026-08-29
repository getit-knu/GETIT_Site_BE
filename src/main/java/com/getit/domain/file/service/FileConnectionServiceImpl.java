package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.StoredObject;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileConnectionServiceImpl implements FileConnectionService {

  private final FileAssetRepository fileAssetRepository;
  private final FileStorage fileStorage;

  @Override
  public void connectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();
    List<FileAsset> files = fileAssetRepository.findAllByIdInAndDeletedAtIsNullForUpdate(distinctFileIds);
    validateAllFound(distinctFileIds, files);

    List<Long> alreadyConnectedFileIds = files.stream()
        .filter(FileAsset::isInUse)
        .map(FileAsset::getId)
        .toList();
    if (!alreadyConnectedFileIds.isEmpty()) {
      log.warn("이미 연결된 파일 재연결 시도. fileIds={}", alreadyConnectedFileIds);
      throw new BusinessException(FileErrorCode.FILE_ALREADY_CONNECTED);
    }

    fileAssetRepository.updateStatusByIdIn(distinctFileIds, FileStatus.CONNECTED);
  }

  @Override
  public void disconnectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();
    List<FileAsset> files = fileAssetRepository.findAllByIdInAndDeletedAtIsNull(distinctFileIds);
    validateAllFound(distinctFileIds, files);

    fileAssetRepository.updateStatusByIdIn(distinctFileIds, FileStatus.PENDING);
  }

  /**
   * 저장소에 실물이 있는지, 신고한 크기를 넘지 않았는지 확인한다.
   *
   * <p>직접 업로드(명세 13.1)는 클라이언트가 신고한 크기로 주소를 발급한다. SAS 자체에는
   * 크기 제한을 걸 수 없어서, 작게 신고하고 크게 올리거나 아예 올리지 않은 채로 fileId 만
   * 연결할 수 있다(PR #126 Copilot 리뷰 지적). 연결은 되돌리기 어려우므로 여기서 막는다.
   *
   * <p>신고 크기는 이미 용도별 상한을 통과했다. 실물이 신고 크기 이하이면 상한도 지켜진다.
   */
  private void verifyUploaded(FileAsset file) {
    Optional<StoredObject> stored = fileStorage.describe(file.getStoredKey());
    if (stored.isEmpty()) {
      log.warn("업로드되지 않은 파일 연결 시도. fileId={} key={}", file.getId(), file.getStoredKey());
      throw new BusinessException(FileErrorCode.FILE_NOT_UPLOADED);
    }
    long actualSize = stored.get().size();
    if (actualSize > file.getSize()) {
      log.warn("신고 크기 초과. fileId={} 신고={} 실제={}", file.getId(), file.getSize(), actualSize);
      throw new BusinessException(FileErrorCode.FILE_SIZE_MISMATCH);
    }
    file.syncSize(actualSize);
  }

  private void validateAllFound(List<Long> requestedFileIds, List<FileAsset> foundFiles) {
    if (foundFiles.size() == requestedFileIds.size()) {
      return;
    }
    Set<Long> foundIds = foundFiles.stream().map(FileAsset::getId).collect(Collectors.toSet());
    List<Long> missingFileIds = requestedFileIds.stream().filter(id -> !foundIds.contains(id)).toList();
    log.warn("존재하지 않는 파일 연결/해제 시도. fileIds={}", missingFileIds);
    throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
  }
}
