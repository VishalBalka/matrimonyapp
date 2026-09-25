package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.RecoveryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCodeEntity, String> {

    List<RecoveryCodeEntity> findByUserIdAndUsedFalse(String userId);

    void deleteByUserId(String userId);
}
