package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.entity.AuditLogEntity;
import com.matrimonyapp.backend.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String userId, String eventType, String details, String ipAddress, String requestId) {
        try {
            AuditLogEntity logEntry = new AuditLogEntity(
                    "audit-" + UUID.randomUUID().toString().substring(0, 12),
                    userId,
                    eventType,
                    details,
                    ipAddress,
                    requestId
            );
            auditLogRepository.save(logEntry);
            log.info("Audit [{}]: User '{}' performed '{}' - {}", requestId, userId != null ? userId : "ANONYMOUS", eventType, details);
        } catch (Exception e) {
            log.error("Failed to persist audit log: {}", e.getMessage());
        }
    }
}
