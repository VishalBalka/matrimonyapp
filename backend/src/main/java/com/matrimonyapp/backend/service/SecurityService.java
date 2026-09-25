package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.dto.security.MfaSetupResponse;
import com.matrimonyapp.backend.dto.security.PrivacyUpdateRequest;
import com.matrimonyapp.backend.dto.security.ReportRequest;
import com.matrimonyapp.backend.dto.security.SecurityStateResponse;
import com.matrimonyapp.backend.entity.*;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.*;
import com.matrimonyapp.backend.security.TotpService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SecurityService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final TotpService totpService;
    private final AuditService auditService;

    public SecurityService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            BlockRepository blockRepository,
            ReportRepository reportRepository,
            RecoveryCodeRepository recoveryCodeRepository,
            TotpService totpService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.totpService = totpService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public SecurityStateResponse getState(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));
        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        return new SecurityStateResponse(
                user.isMfaEnabled(),
                profile.getProfileVisibility(),
                profile.isProfileLocked(),
                profile.isShowPhone(),
                profile.isShowSalary(),
                profile.isShowSocial()
        );
    }

    @Transactional
    public MessageResponse updatePrivacy(String userId, PrivacyUpdateRequest request, String clientIp, String requestId) {
        ProfileEntity profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Profile not found."));

        profile.setProfileVisibility(request.getVisibility() != null ? request.getVisibility() : "PUBLIC");
        profile.setProfileLocked(request.isProfileLocked());
        profile.setShowPhone(request.isShowPhone());
        profile.setShowSalary(request.isShowSalary());
        profile.setShowSocial(request.isShowSocial());
        profileRepository.save(profile);

        auditService.record(userId, "PRIVACY_UPDATED", "Updated privacy visibility settings", clientIp, requestId);
        return new MessageResponse("Privacy settings updated successfully.");
    }

    @Transactional
    public MfaSetupResponse setupMfa(String userId, String clientIp, String requestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        String secret = totpService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        auditService.record(userId, "MFA_SETUP_REQUESTED", "Generated TOTP secret for user", clientIp, requestId);
        return new MfaSetupResponse(
                secret,
                "Scan the QR code or enter this secret into your authenticator app."
        );
    }

    @Transactional
    public MessageResponse enableMfa(String userId, String code, String clientIp, String requestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "MFA setup has not been initiated. Please call /api/v1/security/mfa/setup first.");
        }

        if (!totpService.verifyCode(user.getMfaSecret(), code)) {
            auditService.record(userId, "MFA_ENABLE_FAILED", "Invalid TOTP verification code", clientIp, requestId);
            throw new AppException(ErrorCode.OTP_INVALID, "Invalid verification code. Please check your authenticator app.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        // Generate emergency recovery codes (hashed in database)
        recoveryCodeRepository.deleteByUserId(userId);
        List<String> recoveryCodes = totpService.generateRecoveryCodes(8);
        for (String rc : recoveryCodes) {
            RecoveryCodeEntity entity = new RecoveryCodeEntity(
                    "rec-" + UUID.randomUUID().toString().substring(0, 10),
                    userId,
                    totpService.hashCode(rc)
            );
            recoveryCodeRepository.save(entity);
        }

        auditService.record(userId, "MFA_ENABLED", "Two-factor authentication successfully enabled", clientIp, requestId);
        return new MessageResponse("Two-factor authentication enabled.");
    }

    @Transactional
    public MessageResponse disableMfa(String userId, String code, String clientIp, String requestId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        if (!user.isMfaEnabled()) {
            return new MessageResponse("Two-factor authentication is already disabled.");
        }

        if (!totpService.verifyCode(user.getMfaSecret(), code)) {
            auditService.record(userId, "MFA_DISABLE_FAILED", "Failed attempt to disable MFA", clientIp, requestId);
            throw new AppException(ErrorCode.OTP_INVALID, "Invalid verification code. Cannot disable MFA.");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        recoveryCodeRepository.deleteByUserId(userId);

        auditService.record(userId, "MFA_DISABLED", "Two-factor authentication disabled", clientIp, requestId);
        return new MessageResponse("Two-factor authentication disabled.");
    }

    @Transactional
    public MessageResponse blockUser(String blockerUserId, String targetUserId, String clientIp, String requestId) {
        if (blockerUserId.equals(targetUserId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "You cannot block yourself.");
        }

        if (!blockRepository.existsByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId)) {
            BlockEntity block = new BlockEntity(
                    "blk-" + UUID.randomUUID().toString().substring(0, 10),
                    blockerUserId,
                    targetUserId
            );
            blockRepository.save(block);
        }

        auditService.record(blockerUserId, "USER_BLOCKED", "Blocked user: " + targetUserId, clientIp, requestId);
        return new MessageResponse("User blocked successfully.");
    }

    @Transactional
    public MessageResponse unblockUser(String blockerUserId, String targetUserId, String clientIp, String requestId) {
        blockRepository.deleteByBlockerUserIdAndBlockedUserId(blockerUserId, targetUserId);
        auditService.record(blockerUserId, "USER_UNBLOCKED", "Unblocked user: " + targetUserId, clientIp, requestId);
        return new MessageResponse("User unblocked successfully.");
    }

    @Transactional
    public MessageResponse reportUser(String reporterUserId, String reportedUserId, ReportRequest request, String clientIp, String requestId) {
        if (reporterUserId.equals(reportedUserId)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "You cannot report yourself.");
        }

        ReportEntity report = new ReportEntity(
                "rep-" + UUID.randomUUID().toString().substring(0, 10),
                reporterUserId,
                reportedUserId,
                request.getReason().trim(),
                request.getDetails() != null ? request.getDetails().trim() : null
        );
        reportRepository.save(report);

        auditService.record(reporterUserId, "USER_REPORTED", "Filed report against user: " + reportedUserId + " (Reason: " + request.getReason() + ")", clientIp, requestId);
        return new MessageResponse("Report submitted for administrator review.");
    }
}
