package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.auth.MessageResponse;
import com.matrimonyapp.backend.dto.security.*;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "Security & Privacy", description = "MFA enrollment, privacy toggles, member blocking, and abuse reporting")
public class SecurityController {

    private final SecurityService securityService;

    public SecurityController(SecurityService securityService) {
        this.securityService = securityService;
    }

    @GetMapping
    @Operation(summary = "Get security status", description = "Query MFA activation and visibility preferences")
    public ResponseEntity<SecurityStateResponse> getState(@AuthenticationPrincipal UserPrincipal principal) {
        SecurityStateResponse response = securityService.getState(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/privacy")
    @Operation(summary = "Update privacy preferences", description = "Modify profile visibility, phone, salary, and social links privacy")
    public ResponseEntity<MessageResponse> updatePrivacy(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PrivacyUpdateRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.updatePrivacy(principal.getUserId(), request, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/setup")
    @Operation(summary = "Initiate MFA setup", description = "Generate TOTP secret key for Google Authenticator / 1Password")
    public ResponseEntity<MfaSetupResponse> setupMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MfaSetupResponse response = securityService.setupMfa(principal.getUserId(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/enable")
    @Operation(summary = "Enable MFA", description = "Verify code from authenticator app to activate two-factor authentication and generate recovery codes")
    public ResponseEntity<MessageResponse> enableMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CodeRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.enableMfa(principal.getUserId(), request.getCode(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/mfa/disable")
    @Operation(summary = "Disable MFA", description = "Deactivate two-factor authentication using confirmation code")
    public ResponseEntity<MessageResponse> disableMfa(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CodeRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.disableMfa(principal.getUserId(), request.getCode(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/block/{userId}")
    @Operation(summary = "Block user", description = "Prevent target user from seeing your profile or initiating contact")
    public ResponseEntity<MessageResponse> block(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("userId") String targetUserId,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.blockUser(principal.getUserId(), targetUserId, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/block/{userId}")
    @Operation(summary = "Unblock user", description = "Remove target user from blocked list")
    public ResponseEntity<MessageResponse> unblock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("userId") String targetUserId,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.unblockUser(principal.getUserId(), targetUserId, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/report/{userId}")
    @Operation(summary = "Report abuse", description = "Submit a report against a member for administrative review")
    public ResponseEntity<MessageResponse> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("userId") String targetUserId,
            @Valid @RequestBody ReportRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        MessageResponse response = securityService.reportUser(principal.getUserId(), targetUserId, request, clientIp, requestId);
        return ResponseEntity.ok(response);
    }
}
