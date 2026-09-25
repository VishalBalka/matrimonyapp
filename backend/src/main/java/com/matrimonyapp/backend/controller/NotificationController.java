package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.dto.notification.NotificationResponse;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "In-app notifications and security alerts")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @Operation(summary = "List notifications", description = "Retrieve list of all in-app notifications for authenticated user")
    public ResponseEntity<List<NotificationResponse>> list(@AuthenticationPrincipal UserPrincipal principal) {
        List<NotificationResponse> response = notificationService.getUserNotifications(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification read", description = "Mark specific notification as read")
    public ResponseEntity<MessageResponse> markRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") String notificationId
    ) {
        MessageResponse response = notificationService.markAsRead(notificationId, principal.getUserId());
        return ResponseEntity.ok(response);
    }
}
