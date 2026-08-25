package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FileConnectionServiceImpl implements FileConnectionService {

  private final FileAssetRepository fileAssetRepository;

  @Override
  public void connect(Long fileId) { findFile(fileId).connect(); }

  @Override
  public void disconnect(Long fileId) { findFile(fileId).disconnect(); }

  @Override
  public void connectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();
    List<FileAsset> files = fileAssetRepository.findAllByIdInAndDeletedAtIsNull(distinctFileIds);
    validateAllFound(distinctFileIds, files);

    List<Long> alreadyConnectedFileIds = files.stream()
        .filter(FileAsset::isInUse)
        .map(FileAsset::getId)
        .toList();
    if (!alreadyConnectedFileIds.isEmpty()) {
      throw new BusinessException(
          FileErrorCode.FILE_ALREADY_CONNECTED, "이미 연결된 파일: " + alreadyConnectedFileIds);
    }

    files.forEach(FileAsset::connect);
  }

  @Override
  public void disconnectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();
    List<FileAsset> files = fileAssetRepository.findAllByIdInAndDeletedAtIsNull(distinctFileIds);
    validateAllFound(distinctFileIds, files);

    files.forEach(FileAsset::disconnect);
  }

  private FileAsset findFile(Long fileId) {
    return fileAssetRepository.findByIdAndDeletedAtIsNull(fileId)
        .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
  }

  private void validateAllFound(List<Long> requestedFileIds, List<FileAsset> foundFiles) {
    if (foundFiles.size() == requestedFileIds.size()) {
      return;
    }
    Set<Long> foundIds = foundFiles.stream().map(FileAsset::getId).collect(Collectors.toSet());
    List<Long> missingFileIds = requestedFileIds.stream().filter(id -> !foundIds.contains(id)).toList();
    throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다: " + missingFileIds);
  }
}
