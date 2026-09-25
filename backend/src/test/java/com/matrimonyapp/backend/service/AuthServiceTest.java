package com.matrimonyapp.backend.service;

import com.matrimonyapp.backend.dto.auth.LoginRequest;
import com.matrimonyapp.backend.dto.auth.LoginResponse;
import com.matrimonyapp.backend.dto.auth.RegisterRequest;
import com.matrimonyapp.backend.dto.auth.RegisterResponse;
import com.matrimonyapp.backend.entity.UserEntity;
import com.matrimonyapp.backend.exception.AppException;
import com.matrimonyapp.backend.exception.ErrorCode;
import com.matrimonyapp.backend.repository.*;
import com.matrimonyapp.backend.security.JwtTokenProvider;
import com.matrimonyapp.backend.security.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private ProfileRepository profileRepository;
    private SessionRepository sessionRepository;
    private MfaChallengeRepository mfaChallengeRepository;
    private RecoveryCodeRepository recoveryCodeRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider tokenProvider;
    private TotpService totpService;
    private AuditService auditService;
    private NotificationService notificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        profileRepository = Mockito.mock(ProfileRepository.class);
        sessionRepository = Mockito.mock(SessionRepository.class);
        mfaChallengeRepository = Mockito.mock(MfaChallengeRepository.class);
        recoveryCodeRepository = Mockito.mock(RecoveryCodeRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        tokenProvider = Mockito.mock(JwtTokenProvider.class);
        totpService = Mockito.mock(TotpService.class);
        auditService = Mockito.mock(AuditService.class);
        notificationService = Mockito.mock(NotificationService.class);

        authService = new AuthService(
                userRepository,
                profileRepository,
                sessionRepository,
                mfaChallengeRepository,
                recoveryCodeRepository,
                passwordEncoder,
                tokenProvider,
                totpService,
                auditService,
                notificationService
        );
    }

    @Test
    void register_mismatchedPassword_throwsValidationFailed() {
        RegisterRequest req = new RegisterRequest("Test User", "test@example.com", "Password123!", "Mismatch123!");

        AppException ex = assertThrows(AppException.class, () -> authService.register(req, "127.0.0.1", "req-1"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }

    @Test
    void register_existingEmail_throwsConflict() {
        RegisterRequest req = new RegisterRequest("Test User", "exists@example.com", "Password123!", "Password123!");
        when(userRepository.existsByEmailIgnoreCase("exists@example.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(req, "127.0.0.1", "req-1"));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    void register_validRequest_success() {
        RegisterRequest req = new RegisterRequest("Test User", "new@example.com", "Password123!", "Password123!");
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        RegisterResponse resp = authService.register(req, "127.0.0.1", "req-1");
        assertNotNull(resp.getUserId());
        assertEquals("Account created successfully.", resp.getMessage());
    }

    @Test
    void login_invalidPassword_incrementsFailureAndLocks() {
        UserEntity user = new UserEntity("u1", "test@example.com", passwordEncoder.encode("CorrectPass123!"), "Test User", "MEMBER");
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));

        LoginRequest req = new LoginRequest("test@example.com", "WrongPass123!");
        AppException ex = assertThrows(AppException.class, () -> authService.login(req, "127.0.0.1", "agent", "req-1"));

        assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.getErrorCode());
        assertTrue(user.isAccountLocked(), "Account should be locked after 5 failed attempts");
    }

    @Test
    void login_validCredentials_issuesSession() {
        UserEntity user = new UserEntity("u1", "test@example.com", passwordEncoder.encode("CorrectPass123!"), "Test User", "MEMBER");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(anyString(), anyString(), anyString())).thenReturn("test-jwt-token");
        when(tokenProvider.generateRefreshToken(anyString())).thenReturn("test-refresh-token");
        when(tokenProvider.hashToken(anyString())).thenReturn("hashed-token");
        when(tokenProvider.getExpirationDateFromToken(anyString())).thenReturn(Instant.now().plusSeconds(3600));

        LoginRequest req = new LoginRequest("test@example.com", "CorrectPass123!");
        LoginResponse resp = authService.login(req, "127.0.0.1", "agent", "req-1");

        assertNotNull(resp.getToken());
        assertEquals("u1", resp.getUserId());
        assertEquals("test@example.com", resp.getEmail());
        assertFalse(resp.isMfaRequired());
    }
}
