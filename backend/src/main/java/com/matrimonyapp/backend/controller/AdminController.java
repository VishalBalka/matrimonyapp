package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.admin.AdminDashboardResponse;
import com.matrimonyapp.backend.dto.admin.AdminReportResponse;
import com.matrimonyapp.backend.dto.admin.StatusRequest;
import com.matrimonyapp.backend.dto.admin.VerifyRequest;
import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Administration", description = "Privileged moderation, background checks, profile verification, and account lockout operations")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Admin dashboard", description = "System metrics for active users, profiles, open reports, and verification queue")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        AdminDashboardResponse response = adminService.getDashboard();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reports")
    @Operation(summary = "List reports", description = "Query moderation queue reports optionally filtered by status")
    public ResponseEntity<List<AdminReportResponse>> reports(@RequestParam(value = "status", required = false) String status) {
        List<AdminReportResponse> response = adminService.getReports(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profiles/{profileId}/verify")
    @Operation(summary = "Verify profile", description = "Award or revoke verified badge for a profile")
    public ResponseEntity<MessageResponse> verifyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("profileId") String profileId,
            @RequestBody VerifyRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = adminService.verifyProfile(principal.getUserId(), profileId, request.isVerified(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profiles/{profileId}/background-check")
    @Operation(summary = "Update background check", description = "Set background verification status (UNREQUESTED, PENDING, VERIFIED, REJECTED)")
    public ResponseEntity<MessageResponse> backgroundCheck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("profileId") String profileId,
            @Valid @RequestBody StatusRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = adminService.updateBackgroundCheck(principal.getUserId(), profileId, request.getStatus(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reports/{reportId}/status")
    @Operation(summary = "Update report status", description = "Set report moderation status (OPEN, INVESTIGATING, RESOLVED, DISMISSED)")
    public ResponseEntity<MessageResponse> reportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("reportId") String reportId,
            @Valid @RequestBody StatusRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = adminService.updateReportStatus(principal.getUserId(), reportId, request.getStatus(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/lock")
    @Operation(summary = "Lock/Unlock user account", description = "Suspend or reinstate user account and revoke all sessions")
    public ResponseEntity<MessageResponse> lockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("userId") String targetUserId,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = adminService.lockUser(principal.getUserId(), targetUserId, clientIp, requestId);
        return ResponseEntity.ok(response);
    }
}
