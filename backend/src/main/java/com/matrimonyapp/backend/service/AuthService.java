package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.auth.*;
import com.matrimonyapp.backend.entity.MfaChallengeEntity;
import com.matrimonyapp.backend.entity.ProfileEntity;
import com.matrimonyapp.backend.entity.RecoveryCodeEntity;
import com.matrimonyapp.backend.entity.SessionEntity;
import com.matrimonyapp.backend.entity.UserEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.*;
import com.matrimonyapp.backend.security.JwtTokenProvider;
import com.matrimonyapp.backend.security.TotpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SessionRepository sessionRepository;
    private final MfaChallengeRepository mfaChallengeRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TotpService totpService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AuthService(
            UserRepository userRepository,
            ProfileRepository profileRepository,
            SessionRepository sessionRepository,
            MfaChallengeRepository mfaChallengeRepository,
            RecoveryCodeRepository recoveryCodeRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            TotpService totpService,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.sessionRepository = sessionRepository;
        this.mfaChallengeRepository = mfaChallengeRepository;
        this.recoveryCodeRepository = recoveryCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.totpService = totpService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request, String clientIp, String requestId) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Password and confirm password must match.");
        }

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.CONFLICT, "An account with this email already exists.");
        }

        String userId = "user-" + UUID.randomUUID().toString().substring(0, 10);
        String profileId = "prof-" + UUID.randomUUID().toString().substring(0, 10);

        UserEntity user = new UserEntity(
                userId,
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName().trim(),
                "MEMBER"
        );
        userRepository.save(user);

        ProfileEntity profile = new ProfileEntity();
        profile.setId(profileId);
        profile.setUserId(userId);
        profile.setDisplayName(request.getDisplayName().trim());
        profile.setDateOfBirth("2000-01-01");
        profile.setGender("Unspecified");
        profile.setCountry("India");
        profile.setStateProvince("");
        profile.setCity("Unspecified");
        profile.setBio("New member profile.");
        profile.setProfileVisibility("PUBLIC");
        profileRepository.save(profile);

        auditService.record(userId, "USER_REGISTERED", "Registered user account with email: " + email, clientIp, requestId);
        notificationService.createNotification(userId, "WELCOME", "Welcome to MatrimonyApp", "Complete your profile to find meaningful connections.");

        return new RegisterResponse(userId, "Account created successfully.");
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp, String userAgent, String requestId) {
        String email = request.getEmail().trim().toLowerCase();
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password."));

        Instant now = Instant.now();

        // Check account lock status
        if (user.isAccountLocked()) {
            if (user.getLockoutUntil() != null && now.isAfter(user.getLockoutUntil())) {
                // Lockout period expired -> automatically unlock
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockoutUntil(null);
                userRepository.save(user);
            } else {
                auditService.record(user.getId(), "LOGIN_LOCKED_ATTEMPT", "Attempt to sign in to locked account", clientIp, requestId);
                throw new AppException(ErrorCode.ACCOUNT_LOCKED, "Account is locked due to too many failed attempts or administrative action.");
            }
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLocked(true);
                user.setLockoutUntil(now.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                auditService.record(user.getId(), "ACCOUNT_LOCKED_RATE", "Locked after 5 consecutive failed login attempts", clientIp, requestId);
            }
            userRepository.save(user);
            auditService.record(user.getId(), "LOGIN_FAILED", "Invalid password provided", clientIp, requestId);
            throw new AppException(ErrorCode.AUTH_INVALID_CREDENTIALS, "Invalid email or password.");
        }

        // Password verified -> reset failed attempts
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            userRepository.save(user);
        }

        // Check if MFA is required
        if (user.isMfaEnabled()) {
            String challengeId = "mfa-chal-" + UUID.randomUUID().toString().substring(0, 10);
            MfaChallengeEntity challenge = new MfaChallengeEntity(challengeId, user.getId(), now.plus(5, ChronoUnit.MINUTES));
            mfaChallengeRepository.save(challenge);

            auditService.record(user.getId(), "MFA_CHALLENGE_ISSUED", "MFA code verification requested", clientIp, requestId);
            return LoginResponse.mfaChallenge(user.getId(), user.getEmail(), challengeId);
        }

        // Normal successful login
        return issueSession(user, clientIp, userAgent, requestId);
    }

    @Transactional
    public LoginResponse verifyMfa(MfaVerifyRequest request, String clientIp, String userAgent, String requestId) {
        Instant now = Instant.now();
        MfaChallengeEntity challenge = mfaChallengeRepository
                .findByIdAndConsumedFalseAndExpiresAtAfter(request.getChallengeId(), now)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "MFA challenge has expired or is invalid."));

        UserEntity user = userRepository.findById(challenge.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        boolean verified = false;

        // 1. Check TOTP code against user secret
        if (user.getMfaSecret() != null && totpService.verifyCode(user.getMfaSecret(), request.getCode())) {
            verified = true;
        }

        // 2. Or check single-use recovery code
        if (!verified) {
            String codeHash = totpService.hashCode(request.getCode());
            List<RecoveryCodeEntity> recoveryCodes = recoveryCodeRepository.findByUserIdAndUsedFalse(user.getId());
            for (RecoveryCodeEntity rc : recoveryCodes) {
                if (rc.getCodeHash().equalsIgnoreCase(codeHash)) {
                    rc.setUsed(true);
                    rc.setUsedAt(now);
                    recoveryCodeRepository.save(rc);
                    verified = true;
                    auditService.record(user.getId(), "RECOVERY_CODE_USED", "Used emergency MFA recovery code", clientIp, requestId);
                    break;
                }
            }
        }

        if (!verified) {
            auditService.record(user.getId(), "MFA_VERIFY_FAILED", "Invalid MFA token provided", clientIp, requestId);
            throw new AppException(ErrorCode.OTP_INVALID, "Invalid two-step verification code.");
        }

        challenge.setConsumed(true);
        mfaChallengeRepository.save(challenge);

        auditService.record(user.getId(), "MFA_VERIFY_SUCCESS", "MFA verified successfully", clientIp, requestId);
        return issueSession(user, clientIp, userAgent, requestId);
    }

    @Transactional
    public MessageResponse logout(String token, String userId, String clientIp, String requestId) {
        if (token != null && !token.isBlank()) {
            String tokenHash = tokenProvider.hashToken(token);
            sessionRepository.revokeByTokenHash(tokenHash);
        }
        auditService.record(userId, "USER_LOGOUT", "User logged out successfully", clientIp, requestId);
        return new MessageResponse("Logout successful.");
    }

    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found."));

        return new MeResponse(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.isMfaEnabled()
        );
    }

    private LoginResponse issueSession(UserEntity user, String clientIp, String userAgent, String requestId) {
        String token = tokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        String tokenHash = tokenProvider.hashToken(token);
        String refreshHash = tokenProvider.hashToken(refreshToken);

        Instant expiresAt = tokenProvider.getExpirationDateFromToken(token);

        SessionEntity session = new SessionEntity(
                "sess-" + UUID.randomUUID().toString().substring(0, 10),
                user.getId(),
                tokenHash,
                refreshHash,
                expiresAt,
                clientIp,
                userAgent
        );
        sessionRepository.save(session);

        auditService.record(user.getId(), "LOGIN_SUCCESS", "Session created successfully", clientIp, requestId);

        return new LoginResponse(
                token,
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                expiresAt.toString(),
                false,
                null,
                user.getRole()
        );
    }
}
