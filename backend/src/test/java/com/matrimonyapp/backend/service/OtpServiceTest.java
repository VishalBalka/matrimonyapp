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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class OtpServiceTest {

    private OtpRepository otpRepository;
    private AppProperties appProperties;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        otpRepository = Mockito.mock(OtpRepository.class);
        appProperties = new AppProperties();
        otpService = new OtpService(otpRepository, appProperties);
    }

    @Test
    void requestOtp_cooldownActive_throwsRateLimited() {
        OtpEntity existing = new OtpEntity("o1", "test@example.com", "hash", "REGISTRATION_VERIFICATION", 3, Instant.now().plusSeconds(300));
        when(otpRepository.findByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(anyString(), anyString(), any()))
                .thenReturn(List.of(existing));

        OtpRequest req = new OtpRequest("test@example.com", "REGISTRATION_VERIFICATION");
        AppException ex = assertThrows(AppException.class, () -> otpService.requestOtp(req));
        assertEquals(ErrorCode.RATE_LIMITED, ex.getErrorCode());
    }

    @Test
    void requestOtp_allowed_returnsCooldown() {
        when(otpRepository.findByEmailIgnoreCaseAndPurposeAndCreatedAtAfter(anyString(), anyString(), any()))
                .thenReturn(Collections.emptyList());

        OtpRequest req = new OtpRequest("test@example.com", "REGISTRATION_VERIFICATION");
        OtpResponse resp = otpService.requestOtp(req);
        assertEquals(60, resp.getCooldownSeconds());
    }

    @Test
    void verifyOtp_expiredOrNonExistent_throwsOtpExpired() {
        when(otpRepository.findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        OtpVerifyRequest req = new OtpVerifyRequest("test@example.com", "123456", "REGISTRATION_VERIFICATION");
        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(req));
        assertEquals(ErrorCode.OTP_EXPIRED, ex.getErrorCode());
    }

    @Test
    void verifyOtp_maxAttemptsExceeded_throwsTooManyAttempts() {
        OtpEntity otp = new OtpEntity("o1", "test@example.com", otpService.hashOtp("123456"), "REGISTRATION_VERIFICATION", 3, Instant.now().plusSeconds(300));
        otp.setAttempts(3);
        when(otpRepository.findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.of(otp));

        OtpVerifyRequest req = new OtpVerifyRequest("test@example.com", "123456", "REGISTRATION_VERIFICATION");
        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(req));
        assertEquals(ErrorCode.OTP_TOO_MANY_ATTEMPTS, ex.getErrorCode());
    }

    @Test
    void verifyOtp_invalidCode_incrementsAttemptsAndThrowsOtpInvalid() {
        OtpEntity otp = new OtpEntity("o1", "test@example.com", otpService.hashOtp("654321"), "REGISTRATION_VERIFICATION", 3, Instant.now().plusSeconds(300));
        when(otpRepository.findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.of(otp));

        OtpVerifyRequest req = new OtpVerifyRequest("test@example.com", "000000", "REGISTRATION_VERIFICATION");
        AppException ex = assertThrows(AppException.class, () -> otpService.verifyOtp(req));
        assertEquals(ErrorCode.OTP_INVALID, ex.getErrorCode());
        assertEquals(1, otp.getAttempts());
    }

    @Test
    void verifyOtp_validCode_marksConsumedAndReturnsToken() {
        String correctCode = "489201";
        OtpEntity otp = new OtpEntity("o1", "test@example.com", otpService.hashOtp(correctCode), "REGISTRATION_VERIFICATION", 3, Instant.now().plusSeconds(300));
        when(otpRepository.findTopByEmailIgnoreCaseAndPurposeAndConsumedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), anyString(), any()))
                .thenReturn(Optional.of(otp));

        OtpVerifyRequest req = new OtpVerifyRequest("test@example.com", correctCode, "REGISTRATION_VERIFICATION");
        OtpVerifyResponse resp = otpService.verifyOtp(req);

        assertTrue(resp.isVerified());
        assertNotNull(resp.getVerificationToken());
        assertTrue(otp.isConsumed(), "OTP must be marked consumed to prevent replay");
    }
}
