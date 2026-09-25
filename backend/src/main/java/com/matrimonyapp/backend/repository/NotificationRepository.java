package com.matrimonyapp.backend.repository;

import com.matrimonyapp.backend.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<NotificationEntity> findByIdAndUserId(String id, String userId);
}
