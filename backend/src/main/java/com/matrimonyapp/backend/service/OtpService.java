package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.config.AppProperties;
import com.matrimonyapp.backend.dto.auth.OtpRequest;
import com.matrimonyapp.backend.dto.auth.OtpResponse;
import com.matrimonyapp.backend.dto.auth.OtpVerifyRequest;
import com.matrimonyapp.backend.dto.auth.OtpVerifyResponse;
import com.matrimonyapp.backend.entity.OtpEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.OtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpRepository otpRepository, AppProperties appProperties) {
        this.otpRepository = otpRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public OtpResponse requestOtp(OtpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String purpose = request.getPurpose() != null ? request.getPurpose() : "REGISTRATION_VERIFICATION";

        Instant now = Instant.now();
        Instant cooldownLimit = now.minus(appProperties.getOtp().getCooldownSeconds(), ChronoUnit.SECONDS);

        List<OtpEntity> recentOtps = otpRepository.findByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(email, purpose, cooldownLimit);
        if (!recentOtps.isEmpty()) {
            throw new AppException(ErrorCode.RATE_LIMITED, "Please wait before requesting another verification code.");
        }

        // Generate 6-digit code: 100000 - 999999
        int randomCode = 100000 + secureRandom.nextInt(900000);
        String codeStr = String.valueOf(randomCode);
        String otpHash = hashOtp(codeStr);

        Instant expiresAt = now.plus(appProperties.getOtp().getExpirationMinutes(), ChronoUnit.MINUTES);

        OtpEntity entity = new OtpEntity(
                "otp-" + UUID.randomUUID().toString().substring(0, 10),
                email,
                otpHash,
                purpose,
                appProperties.getOtp().getMaxAttempts(),
                expiresAt
        );
        otpRepository.save(entity);

        return new OtpResponse(
                "If an account exists, a one-time verification code has been dispatched.",
                appProperties.getOtp().getCooldownSeconds()
        );
    }

    @Transactional
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();
        String purpose = request.getPurpose() != null ? request.getPurpose() : "REGISTRATION_VERIFICATION";

        Instant now = Instant.now();

        OtpEntity otpEntity = otpRepository
                .findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, purpose, now)
                .orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED, "Verification code has expired or does not exist."));

        if (otpEntity.getAttempts() >= otpEntity.getMaxAttempts()) {
            throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS, "Maximum verification attempts exceeded. Please request a new code.");
        }

        otpEntity.setAttempts(otpEntity.getAttempts() + 1);

        String inputHash = hashOtp(code);
        if (!constantTimeEquals(inputHash, otpEntity.getOtpHash())) {
            otpRepository.save(otpEntity);
            throw new AppException(ErrorCode.OTP_INVALID, "Invalid verification code.");
        }

        // Mark OTP consumed to prevent reuse
        otpEntity.setConsumed(true);
        otpRepository.save(otpEntity);

        String verificationToken = "verif-" + UUID.randomUUID().toString();
        return new OtpVerifyResponse(true, verificationToken, "Code verified successfully.");
    }

    public String hashOtp(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
