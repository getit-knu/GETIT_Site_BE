package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
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
