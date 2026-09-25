package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<ReportEntity, String> {

    List<ReportEntity> findByStatusOrderByCreatedAtDesc(String status);

    List<ReportEntity> findAllByOrderByCreatedAtDesc();

    long countByStatus(String status);
}
