package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.admin.AdminDashboardResponse;
import com.matrimonyapp.backend.dto.admin.AdminReportResponse;
import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.entity.ReportEntity;
import com.matrimonyapp.backend.entity.UserEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.ProfileRepository;
import com.matrimonyapp.backend.repository.ReportRepository;
import com.matrimonyapp.backend.repository.SessionRepository;
import com.matrimonyapp.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ReportRepository reportRepository;
    private final SessionRepository sessionRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AdminService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            ReportRepository reportRepository,
            SessionRepository sessionRepository,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.reportRepository = reportRepository;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        int usersCount = (int) userRepository.count();
        int profilesCount = (int) profileRepository.count();
        int openReportsCount = (int) reportRepository.countByStatus("OPEN");
        int pendingVerificationCount = (int) profileRepository.countByVerificationStatus("PENDING");

        return new AdminDashboardResponse(usersCount, profilesCount, openReportsCount, pendingVerificationCount);
    }

    @Transactional(readOnly = true)
    public List<AdminReportResponse> getReports(String status) {
        List<ReportEntity> reports;
        if (status != null && !status.isBlank()) {
            reports = reportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase());
        } else {
            reports = reportRepository.findAllByOrderByCreatedAtDesc();
        }

        return reports.stream().map(r -> new AdminReportResponse(
                r.getId(),
                r.getReporterUserId(),
                r.getReportedUserId(),
                r.getReason(),
                r.getDetails(),
                r.getStatus(),
                r.getCreatedAt().toString()
        )).collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse verifyProfile(String adminUserId, String profileId, boolean verified, String clientIp, String requestId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        profile.setVerificationStatus(verified ? "VERIFIED" : "UNVERIFIED");
        profile.setVerifiedAt(verified ? Instant.now() : null);
        profileRepository.save(profile);

        auditService.record(adminUserId, "ADMIN_VERIFY_PROFILE", "Set verification status of profile " + profileId + " to " + verified, clientIp, requestId);
        notificationService.createNotification(
                profile.getUserId(),
                "VERIFICATION_UPDATE",
                "Profile Verification Status Updated",
                verified ? "Your profile has been verified by the moderation team." : "Your profile verification status was removed."
        );

        return new MessageResponse("Profile verification updated successfully.");
    }

    @Transactional
    public MessageResponse updateBackgroundCheck(String adminUserId, String profileId, String status, String clientIp, String requestId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        profile.setBackgroundCheckStatus(status.trim().toUpperCase());
        profileRepository.save(profile);

        auditService.record(adminUserId, "ADMIN_BACKGROUND_CHECK", "Updated background check for profile " + profileId + " to " + status, clientIp, requestId);
        return new MessageResponse("Background check status updated successfully.");
    }

    @Transactional
    public MessageResponse updateReportStatus(String adminUserId, String reportId, String status, String clientIp, String requestId) {
        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Report not found."));

        report.setStatus(status.trim().toUpperCase());
        reportRepository.save(report);

        auditService.record(adminUserId, "ADMIN_REPORT_STATUS", "Updated report " + reportId + " status to " + status, clientIp, requestId);
        return new MessageResponse("Report status updated successfully.");
    }

    @Transactional
    public MessageResponse lockUser(String adminUserId, String targetUserId, String clientIp, String requestId) {
        UserEntity user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        boolean newLockedState = !user.isAccountLocked();
        user.setAccountLocked(newLockedState);
        userRepository.save(user);

        if (newLockedState) {
            // Revoke all active sessions immediately
            sessionRepository.revokeAllByUserId(targetUserId);
        }

        auditService.record(adminUserId, "ADMIN_USER_LOCK", (newLockedState ? "Locked" : "Unlocked") + " user account: " + targetUserId, clientIp, requestId);
        return new MessageResponse("User account " + (newLockedState ? "locked and sessions revoked." : "unlocked."));
    }
}
