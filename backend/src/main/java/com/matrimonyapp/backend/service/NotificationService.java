package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.dto.notification.NotificationResponse;
import com.matrimonyapp.backend.entity.NotificationEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse markAsRead(String notificationId, String userId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Notification not found."));

        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return new MessageResponse("Notification marked as read.");
    }

    @Transactional
    public void createNotification(String userId, String type, String title, String body) {
        NotificationEntity notification = new NotificationEntity(
                "notif-" + UUID.randomUUID().toString().substring(0, 10),
                userId,
                type,
                title,
                body
        );
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(NotificationEntity entity) {
        return new NotificationResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getReadAt() != null ? entity.getReadAt().toString() : null,
                entity.getCreatedAt().toString()
        );
    }
}
