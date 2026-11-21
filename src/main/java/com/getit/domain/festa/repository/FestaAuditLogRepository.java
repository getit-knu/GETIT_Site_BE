package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FestaAuditLogRepository extends JpaRepository<FestaAuditLog, Long> {
}
