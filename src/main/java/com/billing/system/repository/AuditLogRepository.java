package com.billing.system.repository;

import com.billing.system.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
