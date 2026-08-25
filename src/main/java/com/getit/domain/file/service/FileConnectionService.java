package com.getit.domain.file.service;

import java.util.List;

public interface FileConnectionService {

  void connect(Long fileId);

  void disconnect(Long fileId);

  void connectAll(List<Long> fileIds);

  void disconnectAll(List<Long> fileIds);
}
