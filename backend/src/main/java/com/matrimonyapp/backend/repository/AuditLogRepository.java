package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findByUserIdOrderByTimestampDesc(String userId);

    Page<AuditLogEntity> findAllByOrderByTimestampDesc(Pageable pageable);
}
