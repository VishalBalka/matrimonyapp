package com.matrimonyapp.backend.controller;

import com.matrimonyapp.backend.dto.profile.ProfileResponse;
import com.matrimonyapp.backend.dto.profile.ProfileUpdateRequest;
import com.matrimonyapp.backend.entity.PhotoEntity;
import com.matrimonyapp.backend.security.UserPrincipal;
import com.matrimonyapp.backend.service.PhotoService;
import com.matrimonyapp.backend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile", description = "Member personal profile, bio, attributes, and avatar photo management")
public class ProfileController {

    private final ProfileService profileService;
    private final PhotoService photoService;

    public ProfileController(ProfileService profileService, PhotoService photoService) {
        this.profileService = profileService;
        this.photoService = photoService;
    }

    @GetMapping
    @Operation(summary = "Get owner profile", description = "Retrieve full profile information for the authenticated user")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        ProfileResponse response = profileService.getOwnerProfile(principal.getUserId());
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Update profile", description = "Update personal attributes, location, profession, bio, and visibility flags")
    public ResponseEntity<ProfileResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        ProfileResponse response = profileService.updateProfile(principal.getUserId(), request, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile photo", description = "Upload photo image (JPEG/PNG, max 10MB) with magic byte validation")
    public ResponseEntity<ProfileResponse> uploadPhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("photo") MultipartFile photo,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        ProfileResponse response = photoService.uploadPhoto(principal.getUserId(), photo, clientIp, requestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/photo")
    @Operation(summary = "Get owner photo", description = "Download avatar image bytes for authenticated user")
    public ResponseEntity<byte[]> getPhoto(@AuthenticationPrincipal UserPrincipal principal) {
        PhotoEntity photo = photoService.getPhotoByUserId(principal.getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, photo.getContentType())
                .body(photo.getFileData());
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Delete profile photo", description = "Remove current profile photo")
    public ResponseEntity<ProfileResponse> deletePhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest servletRequest
    ) {
        String clientIp = servletRequest.getRemoteAddr();
        String requestId = (String) servletRequest.getAttribute("X-Request-Id");
        ProfileResponse response = photoService.deletePhoto(principal.getUserId(), clientIp, requestId);
        return ResponseEntity.ok(response);
    }
}
