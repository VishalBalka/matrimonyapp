package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.auth.*;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.AuthService;
import com.matrimonyapp.backend.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, credential login, MFA challenges, OTP verification and sessions")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register account", description = "Create a new member account with encrypted password and initial profile")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        RegisterResponse response = authService.register(request, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email and password to receive JWT or MFA challenge")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        LoginResponse response = authService.login(request, clientIp, userAgent, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/verify")
    @Operation(summary = "Verify MFA challenge", description = "Complete login challenge using TOTP authenticator code or recovery code")
    public ResponseEntity<LoginResponse> verifyMfa(
            @Valid @RequestBody MfaVerifyRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = getClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        LoginResponse response = authService.verifyMfa(request, clientIp, userAgent, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidate active session and revoke authentication token")
    public ResponseEntity<MessageResponse> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7).trim() : null;
        String userId = principal != null ? principal.getUserId() : null;
        String clientIp = getClientIp(servletRequest);
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = authService.logout(token, userId, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Current caller", description = "Resolve authenticated user details and active security flags")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        MeResponse response = authService.getCurrentUser(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request OTP", description = "Dispatch one-time verification code with cooldown and attempt limits")
    public ResponseEntity<OtpResponse> requestOtp(@Valid @RequestBody OtpRequest request) {
        OtpResponse response = otpService.requestOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP", description = "Validate one-time code and retrieve verification token")
    public ResponseEntity<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        OtpVerifyResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
